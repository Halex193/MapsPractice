package ro.halex.mapspractice

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.features.*
import org.slf4j.event.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.halex.mapspractice.common.Coordinates
import ro.halex.mapspractice.common.NamedCoordinates
import ro.halex.mapspractice.common.deviceLocationEndpoint
import ro.halex.mapspractice.common.locationsEndPoint
import java.lang.Exception
import java.time.*

val deviceLocations = mutableMapOf<String, Coordinates>()
@ExperimentalCoroutinesApi
val broadcastChannel = BroadcastChannel<Unit>(Channel.CONFLATED)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Locations) {
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        allowCredentials = true
        anyHost()
    }

    install(DataConversion)

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        json()
    }

    install(Compression) {
        gzip()
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!!!!!!!!!!!!", contentType = ContentType.Text.Plain)
        }

        get<MyLocation> {
            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
        }
        // Register nested routes
        get<Type.Edit> {
            call.respondText("Inside $it")
        }
        get<Type.List> {
            call.respondText("Inside $it")
        }

        webSocket(deviceLocationEndpoint){
            incoming.consumeAsFlow()
                .mapNotNull { it as? Frame.Text }
                .map { it.readText() }
                .map { Json.decodeFromString<NamedCoordinates>(it) }
                .collect {deviceLocation ->
                    deviceLocations[deviceLocation.name] = deviceLocation.coordinates
                    broadcastChannel.offer(Unit)
                }
        }
        /*get("/locations") {
            call.respond(deviceLocations)
        }*/
        webSocket(locationsEndPoint) {
            try
            {
                sendDeviceLocations()
                broadcastChannel.openSubscription().consumeEach {
                    sendDeviceLocations()
                }
            } catch (e: ClosedSendChannelException)
            {
                // Ignore
            } catch (e: Exception)
            {
                // Ignore
            }

        }
        static("/") {
            resources("")
        }
    }
}

private suspend fun WebSocketServerSession.sendDeviceLocations()
{
    send(Json.encodeToString(deviceLocations))
}

@KtorExperimentalLocationsAPI
@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")

@KtorExperimentalLocationsAPI
@Location("/type/{name}") data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}

