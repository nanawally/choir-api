package routes

import auth.requireRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ChoristService
import java.util.UUID

@Serializable
data class CreateChoristRequest(val firstName: String, val lastName: String, val isSectionLeader: Boolean = false)

@Serializable
data class UpdateChoristRequest(val firstName: String, val lastName: String, val isSectionLeader: Boolean = false)

@Serializable
data class ChoristResponse(val id: String, val firstName: String, val lastName: String, val isSectionLeader: Boolean, val isArchived: Boolean)

fun Route.choristRoutes() {
    route("/chorists") {
        get {
            val includeArchived = call.request.queryParameters["includeArchived"] == "true"
            val chorists = ChoristService.list(includeArchived).map {
                ChoristResponse(it.id.toString(), it.firstName, it.lastName, it.isSectionLeader, it.isArchived)
            }
            call.respond(chorists)
        }

        post {
            requireRole("admin") ?: return@post
            val req = call.receive<CreateChoristRequest>()
            val chorist = ChoristService.create(req.firstName, req.lastName, req.isSectionLeader)
            call.respond(HttpStatusCode.Created, ChoristResponse(
                chorist.id.toString(), chorist.firstName, chorist.lastName, chorist.isSectionLeader, chorist.isArchived,
            ))
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<UpdateChoristRequest>()
            respondOkOrNotFound(ChoristService.update(id, req.firstName, req.lastName, req.isSectionLeader))
        }

        put("/{id}/archive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            respondOkOrNotFound(ChoristService.archive(id))
        }

        put("/{id}/unarchive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            respondOkOrNotFound(ChoristService.unarchive(id))
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            respondOkOrNotFound(ChoristService.delete(id))
        }
    }
}
