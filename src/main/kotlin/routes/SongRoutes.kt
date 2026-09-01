package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.SongService
import java.util.*

@Serializable
data class CreateSongRequest(val name: String)

@Serializable
data class RenameSongRequest(val name: String)

@Serializable
data class SongResponse(val id: String, val name: String, val sortOrder: Int, val formationId: String?)

fun Route.songRoutes() {
    route("/concerts/{concertId}/songs") {

        get {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val songs = SongService.listByConcert(concertId).map {
                SongResponse(it.id.toString(), it.name, it.sortOrder, it.formationId?.toString())
            }
            call.respond(songs)
        }

        post {
            val req = call.receive<CreateSongRequest>()
            val concertId = UUID.fromString(call.parameters["concertId"])
            val song = SongService.create(concertId, req.name)
            call.respond(
                HttpStatusCode.Created,
                SongResponse(song.id.toString(), song.name, song.sortOrder, null)
            )
        }

        put("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<RenameSongRequest>()
            if (SongService.rename(id, req.name)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            if (SongService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
