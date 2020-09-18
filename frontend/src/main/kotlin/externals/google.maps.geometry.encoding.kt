@file:JsQualifier("google.maps.geometry.encoding")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package google.maps.geometry.encoding

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
import google.maps.LatLng
import google.maps.MVCArray

external fun decodePath(encodedPath: String): Array<LatLng>

external fun encodePath(path: Array<LatLng>): String

external fun encodePath(path: MVCArray<LatLng>): String