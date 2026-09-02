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
data class SongResponse(val id: String, val name: String)

fun Route.songRoutes() {
    route("/songs") {

        get {
            val songs = SongService.list().map { SongResponse(it.id.toString(), it.name) }
            call.respond(songs)
        }

        post {
            val req = call.receive<CreateSongRequest>()
            val song = SongService.create(req.name)
            call.respond(
                HttpStatusCode.Created,
                SongResponse(song.id.toString(), song.name)
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
