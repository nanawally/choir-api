package routes

import auth.extractChoirId
import auth.extractUserId
import auth.requirePermission
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ChoristService
import service.PermissionService
import service.UserService
import java.util.*

@Serializable
data class CreateChoristRequest(val firstName: String, val lastName: String, val isSectionLeader: Boolean = false)

@Serializable
data class UpdateChoristRequest(val firstName: String, val lastName: String, val isSectionLeader: Boolean = false, val notes: String? = null)

@Serializable
data class ChoristResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val isSectionLeader: Boolean,
    val isArchived: Boolean,
    val notes: String? = null,
    val accountStatus: String? = null, // null for non-admins; "none"|"pending"|"active" for admins
)

fun Route.choristRoutes() {
    route("/chorists") {
        get {
            val userId = extractUserId()
            val choirId = extractChoirId()
            val canViewNotes = PermissionService.check(userId, "chorists_notes", "view")
            val canViewAccountStatus = PermissionService.check(userId, "permissions", "view")
            val includeArchived = call.request.queryParameters["includeArchived"] == "true"
            val chorists = ChoristService.list(choirId, includeArchived)
            val accountStatusByChoristId: Map<String, String> = if (canViewAccountStatus) {
                UserService.list(choirId)
                    .filter { it.choristId != null }
                    .associate { it.choristId.toString() to if (it.isPending) "pending" else "active" }
            } else emptyMap()
            call.respond(chorists.map {
                ChoristResponse(
                    id = it.id.toString(),
                    firstName = it.firstName,
                    lastName = it.lastName,
                    isSectionLeader = it.isSectionLeader,
                    isArchived = it.isArchived,
                    notes = if (canViewNotes) it.notes else null,
                    accountStatus = if (canViewAccountStatus) (accountStatusByChoristId[it.id.toString()] ?: "none") else null,
                )
            })
        }

        post {
            if (!requirePermission("chorists", "edit")) return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateChoristRequest>()
            val chorist = ChoristService.create(choirId, req.firstName, req.lastName, req.isSectionLeader)
            call.respond(HttpStatusCode.Created, ChoristResponse(
                chorist.id.toString(), chorist.firstName, chorist.lastName, chorist.isSectionLeader, chorist.isArchived,
            ))
        }

        put("/{id}") {
            if (!requirePermission("chorists", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<UpdateChoristRequest>()
            respondOkOrNotFound(ChoristService.update(id, choirId, req.firstName, req.lastName, req.isSectionLeader, req.notes))
        }

        put("/{id}/archive") {
            if (!requirePermission("chorists", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ChoristService.archive(id, choirId))
        }

        put("/{id}/unarchive") {
            if (!requirePermission("chorists", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ChoristService.unarchive(id, choirId))
        }

        delete("/{id}") {
            if (!requirePermission("chorists", "delete")) return@delete
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ChoristService.delete(id, choirId))
        }

        post("/{id}/account") {
            if (!requirePermission("chorists", "edit")) return@post
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val chorist = ChoristService.getById(id, choirId) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            if (UserService.getByChoristId(id) != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Account already exists for this chorist"))
                return@post
            }
            val (username, token) = UserService.createForChorist(id, choirId, chorist.firstName, chorist.lastName)
            call.respond(HttpStatusCode.Created, mapOf("username" to username, "setupToken" to token, "setupUrl" to "/setup/$token"))
        }
    }
}
