import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.util.*
import ro.halex.mapspractice.common.applicationEndpoint

@KtorExperimentalAPI
val httpClient = HttpClient(Js) {
    install(WebSockets)
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

@KtorExperimentalAPI
suspend fun openLocationsWebSocket(): ClientWebSocketSession
{
    return httpClient.webSocketSession(host = "pcinfosibiu.ro", port = 8000, path = applicationEndpoint)
}

@KtorExperimentalAPI
suspend fun openRoutesWebSocket(): ClientWebSocketSession
{
    return httpClient.webSocketSession(host = "pcinfosibiu.ro", port = 8000, path = "finished-routes")
}