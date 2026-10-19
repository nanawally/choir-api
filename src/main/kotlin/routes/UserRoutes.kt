package routes

import auth.extractChoirId
import auth.requireRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.UserService
import java.util.*

@Serializable
data class CreateUserRequest(val username: String, val role: String = "user")

@Serializable
data class SetRoleRequest(val role: String)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val choristId: String?,
    val role: String,
    val email: String?,
    val isArchived: Boolean,
    val isPending: Boolean,
)

fun Route.userRoutes() {
    route("/users") {
        get {
            requireRole("admin") ?: return@get
            val choirId = extractChoirId()
            val users = UserService.list(choirId).map {
                UserResponse(it.id.toString(), it.username, it.choristId?.toString(),
                    it.role, it.email, it.isArchived, it.isPending)
            }
            call.respond(users)
        }

        post {
            requireRole("admin") ?: return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateUserRequest>()
            val token = UserService.createNonChorist(choirId, req.username, req.role)
            call.respond(HttpStatusCode.Created, mapOf("setupToken" to token, "setupUrl" to "/setup/$token"))
        }

        put("/{id}/role") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<SetRoleRequest>()
            if (req.role !in listOf("admin", "user")) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Role must be 'admin' or 'user'"))
                return@put
            }
            respondOkOrNotFound(UserService.setRole(id, choirId, req.role))
        }

        post("/{id}/reset") {
            requireRole("admin") ?: return@post
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val token = UserService.resetAccount(id, choirId)
            if (token != null) {
                call.respond(mapOf("setupToken" to token, "setupUrl" to "/setup/$token"))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/{id}/archive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(UserService.archive(id, choirId))
        }

        put("/{id}/unarchive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(UserService.unarchive(id, choirId))
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(UserService.delete(id, choirId))
        }
    }
}
