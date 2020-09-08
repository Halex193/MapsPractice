package ro.halex.mapspractice.common

import kotlinx.serialization.Serializable

@Serializable
data class NamedLocation(val name: String, val location: Location)
