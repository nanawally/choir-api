package routes

import auth.extractUserId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.UserService

@Serializable
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class DeleteAccountRequest(val password: String)

@Serializable
data class MeResponse(val username: String, val role: String)

fun Route.meRoutes() {
    route("/me") {
        get {
            val userId = extractUserId()
            val user = UserService.getById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(MeResponse(user.username, user.role))
        }

        put("/password") {
            val userId = extractUserId()
            val req = call.receive<ChangePasswordRequest>()
            if (!UserService.changePassword(userId, req.oldPassword, req.newPassword)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Current password is incorrect"))
                return@put
            }
            call.respond(mapOf("status" to "updated"))
        }

        delete {
            val userId = extractUserId()
            val req = call.receive<DeleteAccountRequest>()
            if (!UserService.deleteSelf(userId, req.password)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Password is incorrect"))
                return@delete
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}
