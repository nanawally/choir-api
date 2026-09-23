package routes

import auth.requireRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ConcertService
import java.util.*

@Serializable
data class CreateConcertRequest(val name: String, val date: String? = null)

@Serializable
data class UpdateConcertRequest(val name: String, val date: String? = null)

@Serializable
data class DuplicateConcertRequest(val name: String)

@Serializable
data class ConcertResponse(val id: String, val name: String, val date: String? = null, val imageUrl: String? = null)

fun Route.concertRoutes() {
    route("/concerts") {
        get {
            val concerts = ConcertService.list().map {
                ConcertResponse(it.id.toString(), it.name, it.date, it.imageUrl)
            }
            call.respond(concerts)
        }

        post {
            requireRole("admin") ?: return@post
            val req = call.receive<CreateConcertRequest>()
            val concert = ConcertService.create(req.name, req.date)
            call.respond(
                HttpStatusCode.Created,
                ConcertResponse(concert.id.toString(), concert.name, concert.date, concert.imageUrl)
            )
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<UpdateConcertRequest>()
            if (ConcertService.update(id, req.name, req.date)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            if (ConcertService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/{id}/duplicate") {
            requireRole("admin") ?: return@post
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<DuplicateConcertRequest>()
            val concert = ConcertService.duplicate(id, req.name)
            if (concert != null) {
                call.respond(HttpStatusCode.Created, ConcertResponse(concert.id.toString(), concert.name, concert.date, concert.imageUrl))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
