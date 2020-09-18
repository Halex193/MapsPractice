package ro.halex.mapspractice

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import ro.halex.mapspractice.common.*
import java.time.Duration

val deviceLocations = mutableMapOf<String, Coordinates>()

@ExperimentalCoroutinesApi
val deviceWaypoints = mutableMapOf<String, BroadcastChannel<Coordinates>>()

@ExperimentalCoroutinesApi
val finishedRoutes = BroadcastChannel<FinishedRoute>(BUFFERED)

@ExperimentalCoroutinesApi
val broadcastChannel = BroadcastChannel<Unit>(CONFLATED)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false)
{
    install(Locations)

    install(DataConversion)

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
        get("/test") {
            call.respondText("Server online", contentType = ContentType.Text.Plain)
        }

        webSocket(deviceEndpoint) {
            try
            {
                val deviceName = (incoming.receive() as Frame.Text).readText()
                launch {
                    incoming.consumeAsFlow()
                        .mapNotNull { it as? Frame.Text }
                        .map { it.readText() }
                        .map { Json.decodeFromString<Coordinates>(it) }
                        .collect { coordinates ->
                            deviceLocations[deviceName] = coordinates
                            broadcastChannel.offer(Unit)
                        }
                }
                val waypointChannel = deviceWaypoints.getOrPut(deviceName) { BroadcastChannel(CONFLATED) }
                waypointChannel.openSubscription().consumeEach { waypoint ->
                    send(Json.encodeToString(waypoint))
                }

            }
            catch (e: ClosedReceiveChannelException)
            {
            }
            catch (e: ClosedSendChannelException)
            {
            }
        }

        webSocket(applicationEndpoint) {
            try
            {
                launch {
                    incoming.consumeAsFlow()
                        .mapNotNull { it as? Frame.Text }
                        .map { it.readText() }
                        .map { Json.decodeFromString<NamedCoordinates>(it) }
                        .collect { namedCoordinates ->
                            deviceWaypoints[namedCoordinates.name]?.send(namedCoordinates.coordinates)
                        }
                }
                broadcastChannel.openSubscription().consumeEach {
                    send(Json.encodeToString(deviceLocations))
                }
            }
            catch (e: ClosedReceiveChannelException)
            {
            }
            catch (e: ClosedSendChannelException)
            {
            }
        }

        webSocket("/finished-routes") {
            finishedRoutes.openSubscription().consumeEach {
                send(Json.encodeToString(it))
            }
        }

        post("/finish-route") {
            call.receive<FinishedRoute>().also { finishedRoutes.send(it) }
            call.respond(HttpStatusCode.OK)
        }

        static("/") {
            files("public")
            default("public/index.html")
        }
    }
}