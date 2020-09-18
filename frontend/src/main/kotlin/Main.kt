import google.maps.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.browser.document
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.*
import ro.halex.mapspractice.common.Coordinates
import ro.halex.mapspractice.common.FinishedRoute
import ro.halex.mapspractice.common.NamedCoordinates
import kotlin.collections.Map
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import google.maps.Map__0 as GoogleMap

private var googleMap = Map(document.getElementById("mapDiv") as HTMLElement, object
{
    val center = object
    {
        val lat = 45.802427
        val lng = 24.1437217
    }
    val zoom = 18
}).unsafeCast<GoogleMap>()

private val mainScope = MainScope()

private val markers: MutableMap<Int, Marker> = mutableMapOf()

private val devices: MutableList<String> = mutableListOf("Choose device")

private val deviceCodes: MutableMap<String, Int> = mutableMapOf("Choose device" to 0)

private val infoWindow = InfoWindow()

private var selectedCoordinates: Coordinates? = null
private var webSocketSession: ClientWebSocketSession? = null


@ExperimentalCoroutinesApi
@KtorExperimentalAPI
suspend fun main()
{
    setupSelectedMarker()
    setupPanel()
    setupButton()
    webSocketSession = openLocationsWebSocket()
    mainScope.launch {
        manageUpdates()
    }
    mainScope.launch {
        routeUpdates()
    }
}

private fun setupButton()
{
    val button = document.getElementById("sendButton") as HTMLDivElement
    button.addEventListener("click", {
        val form = document.forms["current"] as HTMLFormElement
        val radios = form.elements["device"].unsafeCast<RadioNodeList>()
        val deviceCode = radios.value.toInt()
        val coordinates = selectedCoordinates
        if (deviceCode != 0 && coordinates != null)
        {
            mainScope.launch {
                val namedCoordinates = NamedCoordinates(devices[deviceCode], coordinates)
                webSocketSession?.send(Json.encodeToString(namedCoordinates))
                console.log("Sent location: ", namedCoordinates)
            }
        }
    })
}

private val listItems = document.getElementById("list")
private val currentItems = document.getElementById("current")


private fun setupPanel()
{
    currentItems?.innerHTML = currentItem(0, devices[0]) + imageElement()
    listItems?.innerHTML = listItem(0, devices[0])
}

private fun currentItem(deviceCode: Int, text: String): String
{
    return """<div class="select-box__value">
            <input class="select-box__input" type="radio" id="current-$deviceCode" value="$deviceCode" name="device" checked="checked"/>
            <p class="select-box__input-text">$text</p>
        </div>"""
}

private fun listItem(deviceCode: Int, text: String): String
{
    return """<li>
            <label class="select-box__option" id="list-$deviceCode" for="current-$deviceCode" aria-hidden="true">$text</label>
        </li>"""
}

private fun rebuildPanel()
{
    val currentBuilder = StringBuilder(currentItem(0, devices[0]))
    val listBuilder = StringBuilder(listItem(0, devices[0]))

    markers.forEach { (deviceCode, marker) ->
        val deviceName = devices[deviceCode]
        currentBuilder.append(currentItem(deviceCode, deviceName))
        listBuilder.append(listItem(deviceCode, generateListText(deviceName, marker)))
    }
    currentBuilder.append(imageElement())
    currentItems?.innerHTML = currentBuilder.toString()
    listItems?.innerHTML = listBuilder.toString()
}

private fun generateListText(deviceName: String, marker: Marker) =
    "$deviceName - ${marker.getPosition()?.lat()}, ${marker.getPosition()?.lng()}"

private fun imageElement(): String
{
    return """<img class="select-box__icon" src="http://cdn.onlinewebfonts.com/svg/img_295694.svg" alt="Arrow Icon"
             aria-hidden="true"/>"""
}

private fun setupSelectedMarker()
{
    val selectedMarker = Marker(object
    {
        val title = "Selected coordinates"
        val icon = object
        {
            val path = SymbolPath.BACKWARD_CLOSED_ARROW
            val strokeColor = "blue"
            val scale = 5
        }
    })

    googleMap.addListener("click", { e: dynamic ->
        val coordinates = Coordinates(e.latLng.lat(), e.latLng.lng())
        selectedCoordinates = coordinates
        selectedMarker.setMap(googleMap)
        selectedMarker.setPosition(LatLng(coordinates.latitude, coordinates.longitude))
    })
}

@KtorExperimentalAPI
private suspend fun manageUpdates()
{
    val socketSession = webSocketSession ?: return
    console.log("Connected!")
    with(socketSession) {
        incoming.receiveAsFlow()
            .mapNotNull { it as? Frame.Text }
            .map { Json.decodeFromString<Map<String, Coordinates>>(it.readText()) }
            .collect {
                for ((deviceName, coordinates) in it)
                {
                    val latlng = LatLng(coordinates.latitude, coordinates.longitude)
                    val deviceCode = deviceCodes[deviceName]
                    if (deviceCode == null)
                    {
                        //Device does not exist yet
                        devices.add(deviceName)
                        deviceCodes[deviceName] = devices.size - 1
                        markers[devices.size - 1] = Marker(object
                        {
                            val position = latlng
                            val map = googleMap
                            val title = deviceName
                        }).also { marker ->
                            google.maps.event.addListener(marker, "click") {
                                infoWindow.setContent(deviceName)
                                infoWindow.open(googleMap, marker)
                            }
                        }
                        rebuildPanel()
                    }
                    else
                    {
                        val marker = markers[deviceCode]?.apply { setPosition(latlng) } ?: continue
                        document.getElementById("list-$deviceCode")?.textContent =
                            generateListText(deviceName, marker)
                    }
                }
            }
    }
}

@KtorExperimentalAPI
private suspend fun routeUpdates()
{
    openRoutesWebSocket().incoming
        .consumeAsFlow()
        .mapNotNull { it as? Frame.Text }
        .map { it.readText() }
        .map { Json.decodeFromString<FinishedRoute>(it) }
        .collect {
            document.getElementById("notification")?.textContent =
                "${it.deviceName} - ${it.actualTime} / ${it.expectedTime} seconds"
        }
}
