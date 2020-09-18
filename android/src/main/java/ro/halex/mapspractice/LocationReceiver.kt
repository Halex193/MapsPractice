package ro.halex.mapspractice

import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ro.halex.mapspractice.common.Coordinates
import ro.halex.mapspractice.common.FinishedRoute
import java.util.*

private const val TAG = "LocationReceiver"

@ExperimentalCoroutinesApi
class LocationReceiver(
    private val coroutineScope: CoroutineScope,
    private val locationManager: LocationManager
)
{
    var initialLocation: Coordinates? = null
    var initialTime: Instant? = null
    private var marker: Marker? = null
    var map: GoogleMap? = null
    var webSocketSession: ClientWebSocketSession? = null

    fun getDestinationLocation(): LatLng?
    {
        return marker?.position
    }

    fun start()
    {
        val webSocketSession = webSocketSession ?: return
        try
        {
            coroutineScope.launch {
                webSocketSession.incoming
                    .consumeAsFlow()
                    .mapNotNull { it as? Frame.Text }
                    .map { it.readText() }
                    .map { Json.decodeFromString<Coordinates>(it) }
                    .collect { coordinates ->
                        Log.i(TAG, "Received: $coordinates")
                        val position = LatLng(coordinates.latitude, coordinates.longitude)
                        initialLocation = locationManager.lastLocation
                        initialTime = Clock.System.now()
                        map?.apply {
                            val currentMarker = marker
                            if (currentMarker == null)
                                marker = addMarker(MarkerOptions().position(position).title("Waypoint"))
                            else
                                currentMarker.position = position
                    }

                    }
            }
        }
        catch (e: ClosedReceiveChannelException)
        {
            Log.i(TAG, "Location transmission WebSocket disconnected")
        }
        catch (e: Exception)
        {
            Log.e(TAG, e.stackTraceToString())
        }


    }

    fun markAsDone()
    {
        marker?.remove()
        marker = null
    }
}