package routes

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
            val concertId = UUID.fromString(call.parameters["concertId"])
            val concertChorists = ConcertChoristService.listByConcert(concertId).map { it.toString() }
            call.respond(concertChorists)
        }

        put {
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
