package routes

import auth.requirePermission
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ConcertChoristService
import java.util.*

@Serializable
data class SetConcertChoristsRequest(val choristIds: List<String>)

fun Route.concertChoristRoutes() {
    route("/concerts/{concertId}/chorists") {

        get {
            if (!requirePermission("concerts", "view")) return@get
            val concertId = UUID.fromString(call.parameters["concertId"])
            val concertChorists = ConcertChoristService.listByConcert(concertId).map { it.toString() }
            call.respond(concertChorists)
        }

        put {
            if (!requirePermission("concerts", "edit")) return@put
            val concertId = UUID.fromString(call.parameters["concertId"])
            val body = call.receive<SetConcertChoristsRequest>()
            ConcertChoristService.setByConcert(
                concertId,
                body.choristIds.map { UUID.fromString(it) }
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
