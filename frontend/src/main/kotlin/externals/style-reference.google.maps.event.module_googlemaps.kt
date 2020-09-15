@file:JsQualifier("google.maps.event")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package google.maps.event

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
import google.maps.MapsEventListener

external fun addDomListener(instance: Any?, eventName: String, handler: (event: Event) -> Unit, capture: Boolean = definedExternally): MapsEventListener

external fun addDomListenerOnce(instance: Any?, eventName: String, handler: (event: Event) -> Unit, capture: Boolean = definedExternally): MapsEventListener

external fun addListener(instance: Any?, eventName: String, handler: (args: Any) -> Unit): MapsEventListener

external fun addListenerOnce(instance: Any?, eventName: String, handler: (args: Any) -> Unit): MapsEventListener

external fun clearInstanceListeners(instance: Any?)

external fun clearListeners(instance: Any?, eventName: String)

external fun removeListener(listener: MapsEventListener)

external fun trigger(instance: Any, eventName: String, vararg args: Any)