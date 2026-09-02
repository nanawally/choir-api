package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.ConcertSongService
import java.util.*

@Serializable
data class AddSongToConcertRequest(val songId: String)

@Serializable
data class ReorderConcertSongsRequest(val concertSongIds: List<String>)

@Serializable
data class ConcertSongResponse(val id: String, val name: String, val sortOrder: Int)

fun Route.concertSongRoutes() {
    route("/concerts/{concertId}/songs") {

        get {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val songs = ConcertSongService.listByConcert(concertId)
                .map { ConcertSongResponse(it.id.toString(), it.name, it.sortOrder) }
            call.respond(songs)
        }

        post {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val req = call.receive<AddSongToConcertRequest>()
            val song = ConcertSongService.addToConcert(concertId, UUID.fromString(req.songId))
            call.respond(
                HttpStatusCode.Created,
                ConcertSongResponse(song.id.toString(), song.name, song.sortOrder)
            )
        }

        put("/reorder") {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val req = call.receive<ReorderConcertSongsRequest>()
            ConcertSongService.reorder(concertId, req.concertSongIds.map { UUID.fromString(it) })
            call.respond(HttpStatusCode.OK)
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            if (ConcertSongService.removeFromConcert(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
