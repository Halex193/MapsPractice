package ro.halex.mapspractice

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

private const val KEY_CAMERA_POSITION = "camera_position"
private const val KEY_LOCATION = "location"

class LocationManager(val activity: Activity)
{
    var map: GoogleMap? = null
    val DEFAULT_ZOOM = 0.5f
    var PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100
    var locationPermissionGranted = false
    var lastKnownLocation: Location? = null
    var cameraPosition: CameraPosition? = null
    var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

    private val TAG = "LocationManager"

    fun restore(savedInstanceState: Bundle?)
    {
        if (savedInstanceState != null)
        {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
    }

    fun start()
    {
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
    }

    private fun getLocationPermission()
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
        }
        else
        {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    fun checkPermission(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean
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
                }
            }
        }
        updateLocationUI()
        return true
    }

    private fun updateLocationUI()
    {
        val googleMap = map ?: return
        try
        {
            if (locationPermissionGranted)
            {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings?.isMyLocationButtonEnabled = true
            }
            else
            {
                googleMap.isMyLocationEnabled = false
                googleMap.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation()
    {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try
        {
            if (locationPermissionGranted)
            {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful)
                    {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result?.also {
                            map?.addMarker(
                                MarkerOptions().position(
                                    LatLng(
                                        it.latitude,
                                        it.longitude
                                    )
                                ).title("Current Location")
                            )
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        it.latitude,
                                        it.longitude
                                    ), DEFAULT_ZOOM
                                )
                            )
                        }
                    }
                    else
                    {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        /*map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(home, DEFAULT_ZOOM)
                        )*/
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message, e)
        }
    }

    fun save(outState: Bundle)
    {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
    }
}