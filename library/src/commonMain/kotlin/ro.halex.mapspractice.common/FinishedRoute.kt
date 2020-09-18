package ro.halex.mapspractice.common

import kotlinx.serialization.Serializable

@Serializable
data class FinishedRoute(val deviceName: String, val expectedTime: Int, val actualTime: Int)