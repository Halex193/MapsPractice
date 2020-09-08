import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ro.halex.mapspractice.common.Coordinates
import ro.halex.mapspractice.common.NamedCoordinates

val deviceLocations = mutableMapOf<String, Coordinates>()

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
        }
        install(Compression) {
            gzip()
        }

        routing {
            post("/device-location"){
                val deviceLocation = call.receive<NamedCoordinates>()
                deviceLocations[deviceLocation.name] = deviceLocation.coordinates
                call.respond(HttpStatusCode.OK)
            }
            get("/locations") {
                call.respond(deviceLocations)
            }
            get("/") {
                call.respondText(
                        this::class.java.classLoader.getResource("index.html")!!.readText(),
                        ContentType.Text.Html
                )
            }
            static("/") {
                resources("")
            }
        }
    }.start(wait = true)
}