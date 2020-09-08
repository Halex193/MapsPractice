package ro.halex.mapspractice.common

import kotlinx.serialization.Serializable

@Serializable
data class NamedCoordinates(val name: String, val coordinates: Coordinates)
