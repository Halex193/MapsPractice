package ro.halex.mapspractice

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.tasks.Task
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.halex.mapspractice.common.Coordinates
import ro.halex.mapspractice.common.NamedCoordinates
import ro.halex.mapspractice.common.deviceLocationEndpoint
import java.util.*


private const val KEY_CAMERA_POSITION = "camera_position"
private const val KEY_LOCATION = "location"
private const val KEY_REQUESTING_LOCATION_UPDATES: String = "REQUESTING_LOCATION_UPDATES"
private const val REQUEST_CHECK_SETTINGS: Int = 200
private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100
private const val TAG = "LocationManager"

class LocationManager(private val activity: Activity, private val httpClient: HttpClient, private val coroutineScope: CoroutineScope) : LocationCallback()
{
    var map: GoogleMap? = null
    var locationPermissionGranted = false
    var currentLocation: Location? = null
    var cameraPosition: CameraPosition? = null
    var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    var locationRequest: LocationRequest? = null
    var requestingLocationUpdates = false
    private val locationChannel = Channel<Coordinates>(CONFLATED)
    private val deviceName: String = getDeviceName()

    private suspend fun sendLocation()
    {
        try
        {
            httpClient.webSocket(
                    HttpMethod.Get,
                    activity.getString(R.string.location_host),
                    activity.resources.getInteger(R.integer.location_port),
                    deviceLocationEndpoint
            ){
                Log.i(TAG, "Location transmission WebSocket connected")
                for (coordinates in locationChannel)
                {
                    Log.i(TAG, coordinates.toString())
                    send(Json.encodeToString(NamedCoordinates(deviceName, coordinates)))
                }
                Log.i(TAG, "Location transmission WebSocket disconnected")
            }
        } catch (e: ClosedReceiveChannelException)
        {
            Log.i(TAG, "Location transmission WebSocket disconnected")
        } catch (e: Exception)
        {
            Log.e(TAG, e.stackTraceToString())
        }
    }

    override fun onLocationResult(locationResult: LocationResult?)
    {
        locationResult ?: return
        for (location in locationResult.locations)
        {
            locationChannel.offer(Coordinates(location.latitude, location.longitude))
        }
    }

    private fun stopLocationUpdates()
    {
        fusedLocationClient.removeLocationUpdates(this)
        requestingLocationUpdates = false
    }

    fun restore(savedInstanceState: Bundle?)
    {
        savedInstanceState ?: return

        if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES))
        {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    KEY_REQUESTING_LOCATION_UPDATES)
        }
        currentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
    }

    fun start()
    {
        requestLocationPermission()
    }

    fun resume()
    {
        if(requestingLocationUpdates)
            startLocationUpdates()
    }

    private fun requestLocationPermission()
    {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        val checkSelfPermission = ContextCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED)
        {
            locationPermissionGranted = true
            createLocationRequest()
            updateMyLocationUI()
        } else
        {
            ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    fun checkLocationPermission(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ): Boolean
    {
        locationPermissionGranted = false
        when (requestCode)
        {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION ->
            {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                )
                {
                    locationPermissionGranted = true
                    createLocationRequest()
                }
            }
        }
        updateMyLocationUI()
        return true
    }

    private fun updateMyLocationUI()
    {
        val googleMap = map ?: return
        try
        {
            if (locationPermissionGranted)
            {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings?.isMyLocationButtonEnabled = true
            } else
            {
                googleMap.isMyLocationEnabled = false
                googleMap.uiSettings?.isMyLocationButtonEnabled = false
                currentLocation = null
                requestLocationPermission()
            }
        } catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message, e)
        }
    }

    fun save(outState: Bundle)
    {
        outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, currentLocation)
        }
    }

    private fun createLocationRequest()
    {
        if (!locationPermissionGranted)
        {
            requestLocationPermission()
            return
        }
        val create = LocationRequest.create() ?: return
        val locationRequest = create.apply {
            interval = 100
            fastestInterval = 100
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        this.locationRequest = locationRequest
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            requestingLocationUpdates = true
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException)
            {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try
                {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(activity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException)
                {
                    // Ignore the error.
                }
            }
        }
    }

    private fun startLocationUpdates()
    {
        coroutineScope.launch {
            sendLocation()
        }
        try
        {
            if (!locationPermissionGranted) return
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    this,
                    Looper.getMainLooper())
        } catch (e: SecurityException)
        {
            //Ignore the exception
        }
    }

    fun stop()
    {
        stopLocationUpdates()
    }

    private fun getDeviceName(): String
    {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.toLowerCase(Locale.getDefault()).startsWith(manufacturer.toLowerCase(Locale.getDefault())))
        {
            model.capitalize(Locale.getDefault())
        } else
        {
            "${manufacturer.capitalize(Locale.getDefault())} $model"
        }
    }
}