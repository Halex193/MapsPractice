@file:JsQualifier("google.maps.drawing")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package google.maps.drawing

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
import google.maps.Map__0
import google.maps.MVCObject
import google.maps.CircleOptions
import google.maps.MarkerOptions
import google.maps.PolygonOptions
import google.maps.PolylineOptions
import google.maps.RectangleOptions
import google.maps.ControlPosition

external open class DrawingManager(options: DrawingManagerOptions = definedExternally) : MVCObject {
    open fun getDrawingMode(): OverlayType
    open fun getMap(): Map__0
    open fun setDrawingMode(drawingMode: OverlayType?)
    open fun setMap(map: Map__0?)
    open fun setOptions(options: DrawingManagerOptions)
}

external interface DrawingManagerOptions {
    var circleOptions: CircleOptions?
        get() = definedExternally
        set(value) = definedExternally
    var drawingControl: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var drawingControlOptions: DrawingControlOptions?
        get() = definedExternally
        set(value) = definedExternally
    var drawingMode: OverlayType?
        get() = definedExternally
        set(value) = definedExternally
    var map: Map__0?
        get() = definedExternally
        set(value) = definedExternally
    var markerOptions: MarkerOptions?
        get() = definedExternally
        set(value) = definedExternally
    var polygonOptions: PolygonOptions?
        get() = definedExternally
        set(value) = definedExternally
    var polylineOptions: PolylineOptions?
        get() = definedExternally
        set(value) = definedExternally
    var rectangleOptions: RectangleOptions?
        get() = definedExternally
        set(value) = definedExternally
}

external interface DrawingControlOptions {
    var drawingModes: Array<OverlayType>?
        get() = definedExternally
        set(value) = definedExternally
    var position: ControlPosition?
        get() = definedExternally
        set(value) = definedExternally
}

external interface OverlayCompleteEvent {
    var overlay: dynamic /* Marker | Polygon | Polyline | Rectangle | Circle */
        get() = definedExternally
        set(value) = definedExternally
    var type: OverlayType
}

external enum class OverlayType {
    CIRCLE /* = 'circle' */,
    MARKER /* = 'marker' */,
    POLYGON /* = 'polygon' */,
    POLYLINE /* = 'polyline' */,
    RECTANGLE /* = 'rectangle' */
}