import google.maps.LatLng
import google.maps.Marker
import google.maps.ReadonlyMarkerOptions
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.browser.document
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLElement
import ro.halex.mapspractice.common.Coordinates

external var googleMap: google.maps.Map<HTMLElement>

val mainScope = MainScope()

val markers: MutableMap<String, Marker> = mutableMapOf()

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
suspend fun main()
{
    val latlng = LatLng( 45.802427, 24.1437217)

    mainScope.launch {
        manageUpdates()
    }
}

@KtorExperimentalAPI
private suspend fun manageUpdates()
{
    val root = document.getElementById("root") ?: return
    val socketSession = openLocationsWebSocket()
    console.log("Connected!")
    with(socketSession) {
        incoming.receiveAsFlow()
            .mapNotNull { it as? Frame.Text }
            .map { Json.decodeFromString(MapSerializer(String.serializer(), Coordinates.serializer()), it.readText()) }
            .collect {
                buildString {
                    for ((name, coordinates) in it)
                    {
                        val latlng = LatLng(coordinates.latitude, coordinates.longitude)
                        val marker = markers.getOrPut(name) {
                            Marker(object {
                                val position = latlng
                                val map = googleMap
                                val title = name
                            })
                        }
                        marker.setPosition(latlng)

                        append(name)
                        append(" - ")
                        append(coordinates.latitude)
                        append(", ")
                        append(coordinates.longitude)
                        appendLine()
                    }
                }.also { console.log(it) }
            }
    }
}
