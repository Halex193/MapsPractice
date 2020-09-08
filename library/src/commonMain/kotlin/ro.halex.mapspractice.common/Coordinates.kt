package ro.halex.mapspractice.common

import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(val latitude: Double, val longitude: Double)