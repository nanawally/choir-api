package routes

import auth.requireRole
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
data class UpdateSongRequest(
    val name: String,
    val composer: String? = null,
    val arranger: String? = null,
    val delning: String? = null,
    val languages: String? = null,
    val length: String? = null,
    val accompanied: Boolean? = null,
    val instrument: String? = null,
    val year: Int? = null,
    val collectionName: String? = null,
    val soloists: String? = null,
    val hasSheetMusic: Boolean = false,
)

@Serializable
data class SongResponse(
    val id: String,
    val name: String,
    val composer: String? = null,
    val arranger: String? = null,
    val delning: String? = null,
    val languages: String? = null,
    val length: String? = null,
    val accompanied: Boolean? = null,
    val instrument: String? = null,
    val year: Int? = null,
    val collectionName: String? = null,
    val soloists: String? = null,
    val hasSheetMusic: Boolean = false,
)

private fun toResponse(dto: service.SongDTO) = SongResponse(
    id = dto.id.toString(),
    name = dto.name,
    composer = dto.composer,
    arranger = dto.arranger,
    delning = dto.delning,
    languages = dto.languages,
    length = dto.length,
    accompanied = dto.accompanied,
    instrument = dto.instrument,
    year = dto.year,
    collectionName = dto.collectionName,
    soloists = dto.soloists,
    hasSheetMusic = dto.hasSheetMusic,
)

fun Route.songRoutes() {
    route("/songs") {

        get {
            val songs = SongService.list().map(::toResponse)
            call.respond(songs)
        }

        post {
            requireRole("admin") ?: return@post
            val req = call.receive<CreateSongRequest>()
            val song = SongService.create(req.name)
            call.respond(HttpStatusCode.Created, toResponse(song))
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<UpdateSongRequest>()
            if (SongService.update(id, req.name, req.composer, req.arranger, req.delning,
                    req.languages, req.length, req.accompanied, req.instrument,
                    req.year, req.collectionName, req.soloists, req.hasSheetMusic)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            if (SongService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
