package ro.halex.mapspractice

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val DEFAULT_ZOOM = 0.5f

@KtorExperimentalAPI
class MapsActivity : AppCompatActivity(), OnMapReadyCallback
{
    private lateinit var locationManager: LocationManager
    private var map: GoogleMap? = null
    private var socketSession: ClientWebSocketSession? = null

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
        locationManager = LocationManager(this, httpClient, lifecycleScope)
        locationManager.restore(savedInstanceState)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        /*lifecycleScope.launch {
            with(connectWebSocket())
            {
                socketSession = this
                getLocations { text ->
                    val coordinates = Json.decodeFromString<NamedCoordinates>(text)
                    Toast.makeText(this@MapsActivity, coordinates.name, Toast.LENGTH_SHORT).show()
                    val latLng = coordinates.toLatLng()
                    map?.addMarker(MarkerOptions().position(latLng).title(coordinates.name))
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                }
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        locationManager.resume()
    }

    override fun onStop()
    {
        locationManager.stop()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle item selection
        return when (item.itemId)
        {
            R.id.next ->
            {
                lifecycleScope.launch {
                    socketSession?.send(Frame.Text("next please!"))
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
}

