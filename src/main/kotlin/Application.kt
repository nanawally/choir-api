import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import routes.choristRoutes
import routes.formationRoutes
import routes.voiceGroupRoutes

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
        anyHost()
    }

    configureDatabase()

    routing {
        get("/health") {
            call.respondText("OK")
        }
        choristRoutes()
        formationRoutes()
        voiceGroupRoutes()
    }
}
