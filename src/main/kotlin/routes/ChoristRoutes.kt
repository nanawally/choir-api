package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ChoristService
import java.util.UUID

@Serializable
data class CreateChoristRequest(val name: String)

@Serializable
data class RenameChoristRequest(val name: String)

@Serializable
data class ChoristResponse(val id: String, val name: String)

fun Route.choristRoutes() {
    route("/chorists") {
        get {
            val chorists = ChoristService.list().map {
                ChoristResponse(it.id.toString(), it.name)
            }
            call.respond(chorists)
        }

        post {
            val req = call.receive<CreateChoristRequest>()
            val chorist = ChoristService.create(req.name)
            call.respond(HttpStatusCode.Created, ChoristResponse(chorist.id.toString(), chorist.name))
        }

        put("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<RenameChoristRequest>()
            if (ChoristService.rename(id, req.name)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            if (ChoristService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
