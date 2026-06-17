package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ConcertService
import java.util.*

@Serializable
data class CreateConcertRequest(val name: String)

@Serializable
data class RenameConcertRequest(val name: String)

@Serializable
data class DuplicateConcertRequest(val name: String)

@Serializable
data class ConcertResponse(val id: String, val name: String)

fun Route.concertRoutes() {
    route("/concerts") {
        get {
            val concerts = ConcertService.list().map {
                ConcertResponse(it.id.toString(), it.name)
            }
            call.respond(concerts)
        }

        post {
            val req = call.receive<CreateConcertRequest>()
            val concert = ConcertService.create(req.name)
            call.respond(
                HttpStatusCode.Created,
                ConcertResponse(concert.id.toString(), concert.name)
            )
        }

        put("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<RenameConcertRequest>()
            if (ConcertService.rename(id, req.name)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            if (ConcertService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/{id}/duplicate") {
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<DuplicateConcertRequest>()
            val concert = ConcertService.duplicate(id, req.name)
            if (concert != null) {
                call.respond(HttpStatusCode.Created, ConcertResponse(concert.id.toString(), concert.name))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
