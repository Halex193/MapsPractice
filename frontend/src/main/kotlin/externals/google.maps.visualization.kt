@file:JsQualifier("google.maps.visualization")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package google.maps.visualization

import kotlin.js.*
import kotlin.js.Json
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*
import google.maps.MVCArray
import google.maps.Map__0
import google.maps.LatLng
import google.maps.MVCObject
import google.maps.Size

external open class HeatmapLayer(opts: HeatmapLayerOptions = definedExternally) : MVCObject {
    open fun getData(): MVCArray<dynamic /* LatLng | WeightedLocation */>
    open fun getMap(): Map__0
    open fun setData(data: MVCArray<dynamic /* LatLng | WeightedLocation */>)
    open fun setData(data: Array<LatLng>)
    open fun setData(data: Array<WeightedLocation>)
    open fun setMap(map: Map__0?)
    open fun setOptions(options: HeatmapLayerOptions)
}

external interface HeatmapLayerOptions {
    var data: Any
    var dissipating: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var gradient: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var map: Map__0?
        get() = definedExternally
        set(value) = definedExternally
    var maxIntensity: Number?
        get() = definedExternally
        set(value) = definedExternally
    var opacity: Number?
        get() = definedExternally
        set(value) = definedExternally
    var radius: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface WeightedLocation {
    var location: LatLng
    var weight: Number
}

external open class MapsEngineLayer(options: MapsEngineLayerOptions) : MVCObject {
    open fun getLayerId(): String
    open fun getLayerKey(): String
    open fun getMap(): Map__0
    open fun getMapId(): String
    open fun getOpacity(): Number
    open fun getProperties(): MapsEngineLayerProperties
    open fun getStatus(): MapsEngineStatus
    open fun getZIndex(): Number
    open fun setLayerId(layerId: String)
    open fun setLayerKey(layerKey: String)
    open fun setMap(map: Map__0?)
    open fun setMapId(mapId: String)
    open fun setOpacity(opacity: Number)
    open fun setOptions(options: MapsEngineLayerOptions)
    open fun setZIndex(zIndex: Number)
}

external interface MapsEngineLayerOptions {
    var accessToken: String?
        get() = definedExternally
        set(value) = definedExternally
    var clickable: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var fitBounds: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var layerId: String?
        get() = definedExternally
        set(value) = definedExternally
    var layerKey: String?
        get() = definedExternally
        set(value) = definedExternally
    var map: Map__0?
        get() = definedExternally
        set(value) = definedExternally
    var mapId: String?
        get() = definedExternally
        set(value) = definedExternally
    var opacity: Number?
        get() = definedExternally
        set(value) = definedExternally
    var suppressInfoWindows: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var zIndex: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface MapsEngineLayerProperties {
    var name: String
}

external interface MapsEngineMouseEvent {
    var featureId: String?
        get() = definedExternally
        set(value) = definedExternally
    var infoWindowHtml: String?
        get() = definedExternally
        set(value) = definedExternally
    var latLng: LatLng?
        get() = definedExternally
        set(value) = definedExternally
    var pixelOffset: Size?
        get() = definedExternally
        set(value) = definedExternally
}

external enum class MapsEngineStatus {
    INVALID_LAYER /* = 'INVALID_LAYER' */,
    OK /* = 'OK' */,
    UNKNOWN_ERROR /* = 'UNKNOWN_ERROR' */
}

external open class MouseEvent {
    open fun stop()
}

external open class MapsEventListener