package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.UserService

@Serializable
data class ClaimAccountRequest(val password: String)

fun Route.setupRoutes() {
    route("/setup") {
        get("/{token}") {
            val token = call.parameters["token"] ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val info = UserService.lookupBySetupToken(token)
            if (info == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invalid or expired token"))
            } else {
                call.respond(mapOf("username" to info.username))
            }
        }

        post("/{token}") {
            val token = call.parameters["token"] ?: run {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val req = call.receive<ClaimAccountRequest>()
            if (UserService.claimAccount(token, req.password)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invalid or expired token"))
            }
        }
    }
}
