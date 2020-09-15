import google.maps.MapOptions
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import ro.halex.mapspractice.common.Coordinates

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
suspend fun main()
{
    manageUpdates()
}

var map: google.maps.Map<HTMLElement>? = null

fun initializeMap()
{
    map = google.maps.Map(document.getElementById("mapDiv") as HTMLElement, object: MapOptions {
        override var center: dynamic = object {
            val lat = -34.397
            val long = 150.644
        }
        override var zoom: Number? = 8
    })
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
            .map {
                buildString {
                    for ((name, coordinates) in it)
                    {
                        append(name)
                        append(" - ")
                        append(coordinates.latitude)
                        append(", ")
                        append(coordinates.longitude)
                        appendLine("<br/>")
                    }
                }
            }
            .collect {
                root.innerHTML = it
                console.log(it)
            }
    }
}
