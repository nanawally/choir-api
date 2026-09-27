import auth.configureAuth
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import routes.*
import service.StorageService

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }

    configureAuth()
    configureDatabase()

    val storage = try {
        val config = environment.config
        StorageService(
            endpoint = config.property("storage.endpoint").getString(),
            accessKey = config.property("storage.accessKey").getString(),
            secretKey = config.property("storage.secretKey").getString(),
            region = config.property("storage.region").getString(),
            bucket = config.property("storage.bucket").getString(),
        )
    } catch (_: Exception) {
        null // Storage not configured — sheet music upload disabled
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }
        authRoutes()

        authenticate("auth-jwt") {
            choristRoutes()
            concertRoutes()
            concertChoristRoutes()
            concertSongRoutes()
            songRoutes(storage)
            formationRoutes()
            songFormationRoutes()
            voiceGroupRoutes()
        }
    }
}
