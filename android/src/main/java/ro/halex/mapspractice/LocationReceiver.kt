package ro.halex.mapspractice

import android.util.Log
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class LocationReceiver(private val httpClient: HttpClient)
{
    private suspend fun connectWebSocket(): ClientWebSocketSession
    {
        return httpClient.webSocketSession(
                HttpMethod.Get,
                "192.168.0.145",
                8080,
                "/locations"
        ).also { Log.i("MapsActivity", "WebSocket connected") }
    }


    private suspend inline fun ClientWebSocketSession.getLocations(crossinline block: suspend (String) -> Unit)
    {
        try
        {
            incoming.consumeAsFlow()
                    .mapNotNull { it as? Frame.Text }
                    .map { it.readText() }
                    .collect { text ->
                        Log.d("MapsActivity", "WebSocket received $text")
                        block(text)
                    }
        } catch (e: ClosedReceiveChannelException)
        {
            Log.i("MapsActivity", "WebSocket disconnected")
        } catch (e: Exception)
        {
            Log.e("MapsActivity", e.stackTraceToString())
        }
    }
}