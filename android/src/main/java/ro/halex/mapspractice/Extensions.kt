package ro.halex.mapspractice

import com.google.android.gms.maps.model.LatLng
import ro.halex.mapspractice.common.NamedCoordinates

fun NamedCoordinates.toLatLng(): LatLng = with(coordinates) {
    return LatLng(latitude, longitude)
}
