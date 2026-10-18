package routes

import auth.extractChoirId
import auth.requireRole
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ChoristService
import java.util.*

@Serializable
data class CreateChoristRequest(val firstName: String, val lastName: String, val isSectionLeader: Boolean = false)

@Serializable
data class UpdateChoristRequest(val firstName: String, val lastName: String, val isSectionLeader: Boolean = false, val notes: String? = null)

@Serializable
data class ChoristResponse(val id: String, val firstName: String, val lastName: String, val isSectionLeader: Boolean, val isArchived: Boolean, val notes: String? = null)

fun Route.choristRoutes() {
    route("/chorists") {
        get {
            val includeArchived = call.request.queryParameters["includeArchived"] == "true"
            val isAdmin = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() == "admin"
            val choirId = extractChoirId()
            val chorists = ChoristService.list(choirId, includeArchived).map {
                ChoristResponse(it.id.toString(), it.firstName, it.lastName, it.isSectionLeader, it.isArchived,
                    notes = if (isAdmin) it.notes else null)
            }
            call.respond(chorists)
        }

        post {
            requireRole("admin") ?: return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateChoristRequest>()
            val chorist = ChoristService.create(choirId, req.firstName, req.lastName, req.isSectionLeader)
            call.respond(HttpStatusCode.Created, ChoristResponse(
                chorist.id.toString(), chorist.firstName, chorist.lastName, chorist.isSectionLeader, chorist.isArchived, chorist.notes,
            ))
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<UpdateChoristRequest>()
            respondOkOrNotFound(ChoristService.update(id, choirId, req.firstName, req.lastName, req.isSectionLeader, req.notes))
        }

        put("/{id}/archive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ChoristService.archive(id, choirId))
        }

        put("/{id}/unarchive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ChoristService.unarchive(id, choirId))
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ChoristService.delete(id, choirId))
        }
    }
}
