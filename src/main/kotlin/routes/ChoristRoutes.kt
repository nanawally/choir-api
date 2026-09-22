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
data class CreateChoristRequest(val name: String, val isSectionLeader: Boolean = false)

@Serializable
data class UpdateChoristRequest(val name: String, val isSectionLeader: Boolean = false)

@Serializable
data class ChoristResponse(val id: String, val name: String, val isSectionLeader: Boolean, val isArchived: Boolean)

fun Route.choristRoutes() {
    route("/chorists") {
        get {
            val includeArchived = call.request.queryParameters["includeArchived"] == "true"
            val chorists = ChoristService.list(includeArchived).map {
                ChoristResponse(it.id.toString(), it.name, it.isSectionLeader, it.isArchived)
            }
            call.respond(chorists)
        }

        post {
            requireRole("admin") ?: return@post
            val req = call.receive<CreateChoristRequest>()
            val chorist = ChoristService.create(req.name, req.isSectionLeader)
            call.respond(HttpStatusCode.Created, ChoristResponse(
                chorist.id.toString(), chorist.name, chorist.isSectionLeader, chorist.isArchived,
            ))
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<UpdateChoristRequest>()
            if (ChoristService.update(id, req.name, req.isSectionLeader)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/{id}/archive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            if (ChoristService.archive(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/{id}/unarchive") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            if (ChoristService.unarchive(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            if (ChoristService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
