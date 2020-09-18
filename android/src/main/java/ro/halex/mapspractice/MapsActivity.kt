package ro.halex.mapspractice

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import ro.halex.mapspractice.common.FinishedRoute
import ro.halex.mapspractice.common.deviceEndpoint
import java.time.Duration
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.time.ExperimentalTime


private const val TAG = "MainActivity"

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class MapsActivity : AppCompatActivity(), OnMapReadyCallback
{
    private lateinit var locationManager: LocationManager
    private var map: GoogleMap? = null
    private val deviceName: String = getDeviceName()
    private lateinit var locationReceiver: LocationReceiver
    private var webSocketSession: ClientWebSocketSession? = null

    private val httpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json { ignoreUnknownKeys = true })
        }
        install(WebSockets)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        locationManager = LocationManager(this@MapsActivity, lifecycleScope)
        locationManager.restore(savedInstanceState)
        locationReceiver = LocationReceiver(lifecycleScope, locationManager)
        lifecycleScope.launch { openWebSocket() }
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onResume()
    {
        super.onResume()
        locationManager.resume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    @ExperimentalTime
    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle item selection
        return when (item.itemId)
        {
            R.id.next ->
            {
                val destination = locationReceiver.getDestinationLocation() ?: return true
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}&zoom=15&center=${destination.latitude},${destination.longitude}")
                )
                startActivity(intent)
                true
            }
            R.id.done ->
            {
                val initialLocation = locationReceiver.initialLocation ?: return true
                val initialTime = locationReceiver.initialTime ?: return true
                val destination = locationReceiver.getDestinationLocation() ?: return true
                val elapsedTime = Clock.System.now() - initialTime
                locationReceiver.markAsDone()
                lifecycleScope.launch {
                    val urlString =
                        "https://maps.googleapis.com/maps/api/distancematrix/json?units=metric&origins=${initialLocation.latitude},${initialLocation.longitude}&destinations=${destination.latitude},${destination.longitude}&key=${
                            getString(R.string.google_maps_key)
                        }"
                    val response = httpClient.get<String>(urlString)
                    Log.d("$TAG- response", response)

                    val jsonElement = Json.parseToJsonElement(response)
                    val duration = jsonElement
                        .jsonObject["rows"]
                        ?.jsonArray?.get(0)
                        ?.jsonObject?.get("elements")
                        ?.jsonArray?.get(0)
                        ?.jsonObject?.get("duration")
                        ?.jsonObject?.get("value")
                        ?.jsonPrimitive?.intOrNull
                        ?: return@launch
                    Log.d("$TAG- duration", duration.toString())

                    val finishedRoute =
                        FinishedRoute(deviceName, duration, elapsedTime.inSeconds.toInt())
                    httpClient.post<Unit>(
                        host = getString(R.string.location_host),
                        port = resources.getInteger(R.integer.location_port),
                        path = "/finish-route"
                    ) {
                        body = finishedRoute
                        contentType(ContentType.Application.Json)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        httpClient.close()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        locationManager.map = googleMap
        locationManager.start()
        locationReceiver.map = googleMap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    )
    {
        locationManager.checkLocationPermission(requestCode, permissions, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        locationManager.save(outState)
        super.onSaveInstanceState(outState)
    }

    private suspend fun openWebSocket()
    {
        Log.i(TAG, "Renewing websocket")
        webSocketSession?.cancel(CancellationException("Renewing connection"))
        webSocketSession = httpClient.webSocketSession(
            HttpMethod.Get,
            getString(R.string.location_host),
            resources.getInteger(R.integer.location_port),
            deviceEndpoint
        ).also {
            it.send(deviceName)
            locationManager.webSocketSession = it
            locationManager.resume()
            locationReceiver.webSocketSession = it
            locationReceiver.start()
        }

    }

    private fun getDeviceName(): String
    {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.toLowerCase(Locale.getDefault())
                .startsWith(manufacturer.toLowerCase(Locale.getDefault()))
        )
        {
            model.capitalize(Locale.getDefault())
        }
        else
        {
            "${manufacturer.capitalize(Locale.getDefault())} $model"
        }
    }

}

