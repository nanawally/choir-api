package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.AuthService

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

fun Route.authRoutes() {
    val config = application.environment.config
    val jwtSecret = config.property("jwt.secret").getString()
    val jwtIssuer = config.property("jwt.issuer").getString()
    val jwtAudience = config.property("jwt.audience").getString()

    post("/hash") {
        val req = call.receive<LoginRequest>()
        val hash = AuthService.hashPassword(req.password)
        call.respond(mapOf("hash" to hash))
    }

    post("/login") {
        val req = call.receive<LoginRequest>()
        val token = AuthService.authenticate(req.username, req.password, jwtSecret, jwtIssuer, jwtAudience)
        if (token != null) {
            call.respond(LoginResponse(token))
        } else {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
        }
    }
}
