package routes

import auth.extractChoirId
import auth.requirePermission
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ChoristService
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
    val choristName: String?,
    val role: String,
    val email: String?,
    val isArchived: Boolean,
    val isPending: Boolean,
)

fun Route.userRoutes() {
    route("/users") {
        get {
            if (!requirePermission("users", "view")) return@get
            val choirId = extractChoirId()
            val users = UserService.list(choirId)
            val choristIds = users.mapNotNull { it.choristId }
            val choristNames = if (choristIds.isNotEmpty()) {
                choristIds.mapNotNull { cId ->
                    ChoristService.getById(cId, choirId)?.let { cId to "${it.firstName} ${it.lastName}" }
                }.toMap()
            } else emptyMap()
            call.respond(users.map {
                UserResponse(it.id.toString(), it.username, it.choristId?.toString(),
                    it.choristId?.let { cId -> choristNames[cId] },
                    it.role, it.email, it.isArchived, it.isPending)
            })
        }

        post {
            if (!requirePermission("users", "edit")) return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateUserRequest>()
            val token = UserService.createNonChorist(choirId, req.username, req.role)
            call.respond(HttpStatusCode.Created, mapOf("setupToken" to token, "setupUrl" to "/setup/$token"))
        }

        put("/{id}/role") {
            if (!requirePermission("users", "edit")) return@put
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
            if (!requirePermission("users", "edit")) return@post
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
            if (!requirePermission("users", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(UserService.archive(id, choirId))
        }

        put("/{id}/unarchive") {
            if (!requirePermission("users", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(UserService.unarchive(id, choirId))
        }

        delete("/{id}") {
            if (!requirePermission("users", "delete")) return@delete
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val alsoDeleteChorist = call.request.queryParameters["deleteChorist"] == "true"
            val user = UserService.getById(id) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            // Delete user first, then chorist (which would otherwise cascade-delete the user)
            UserService.delete(id, choirId)
            if (alsoDeleteChorist && user.choristId != null) {
                ChoristService.delete(user.choristId, choirId, deleteUser = false)
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}
