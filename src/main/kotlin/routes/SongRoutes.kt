package routes

import auth.requireRole
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.utils.io.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.SongService
import service.StorageService
import java.util.*

@Serializable
data class CreateSongRequest(val name: String)

@Serializable
data class UpdateSongRequest(
    val name: String,
    val composer: String? = null,
    val arranger: String? = null,
    val lyricist: String? = null,
    val delning: String? = null,
    val languages: String? = null,
    val length: String? = null,
    val accompanied: Boolean? = null,
    val instrument: String? = null,
    val year: Int? = null,
    val collectionName: String? = null,
    val hasSoloists: Boolean? = null,
    val soloistNames: String? = null,
    val hasSheetMusic: Boolean = false,
    val lyrics: String? = null,
)

@Serializable
data class SongResponse(
    val id: String,
    val name: String,
    val composer: String? = null,
    val arranger: String? = null,
    val lyricist: String? = null,
    val delning: String? = null,
    val languages: String? = null,
    val length: String? = null,
    val accompanied: Boolean? = null,
    val instrument: String? = null,
    val year: Int? = null,
    val collectionName: String? = null,
    val hasSoloists: Boolean? = null,
    val soloistNames: String? = null,
    val hasSheetMusic: Boolean = false,
    val hasSheetMusicFile: Boolean = false,
    val lyrics: String? = null,
)

private fun toResponse(dto: service.SongDTO) = SongResponse(
    id = dto.id.toString(),
    name = dto.name,
    composer = dto.composer,
    arranger = dto.arranger,
    lyricist = dto.lyricist,
    delning = dto.delning,
    languages = dto.languages,
    length = dto.length,
    accompanied = dto.accompanied,
    instrument = dto.instrument,
    year = dto.year,
    collectionName = dto.collectionName,
    hasSoloists = dto.hasSoloists,
    soloistNames = dto.soloistNames,
    hasSheetMusic = dto.hasSheetMusic,
    hasSheetMusicFile = dto.sheetMusicKey != null,
    lyrics = dto.lyrics,
)

fun Route.songRoutes(storage: StorageService?) {
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

        get("/{id}/concerts") {
            val id = UUID.fromString(call.parameters["id"])
            val concerts = SongService.listConcertsForSong(id).map { (cId, name) ->
                mapOf("id" to cId.toString(), "name" to name)
            }
            call.respond(concerts)
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<UpdateSongRequest>()
            if (SongService.update(id, req.name, req.composer, req.arranger, req.lyricist, req.delning,
                    req.languages, req.length, req.accompanied, req.instrument,
                    req.year, req.collectionName, req.hasSoloists, req.soloistNames, req.hasSheetMusic,
                    req.lyrics)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            // Delete sheet music file from storage if it exists
            val key = SongService.getSheetMusicKey(id)
            if (key != null && storage != null) {
                storage.delete(key)
            }
            if (SongService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/{id}/sheet-music") {
            requireRole("admin") ?: return@post
            if (storage == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, "Storage not configured")
                return@post
            }
            val id = UUID.fromString(call.parameters["id"])
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "file") {
                    fileBytes = part.provider().toByteArray()
                }
                part.dispose()
            }

            val bytes = fileBytes
            if (bytes == null) {
                call.respond(HttpStatusCode.BadRequest, "No file provided")
                return@post
            }

            // Compress PDF with Ghostscript
            val compressed = StorageService.compressPdf(bytes)

            // Delete old file if replacing
            val oldKey = SongService.getSheetMusicKey(id)
            if (oldKey != null) {
                storage.delete(oldKey)
            }

            val key = "sheet-music/$id.pdf"
            storage.upload(key, compressed, "application/pdf")
            SongService.setSheetMusicKey(id, key)

            call.respond(HttpStatusCode.OK, mapOf("key" to key, "originalSize" to bytes.size, "compressedSize" to compressed.size))
        }

        get("/{id}/sheet-music") {
            if (storage == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, "Storage not configured")
                return@get
            }
            val id = UUID.fromString(call.parameters["id"])
            val key = SongService.getSheetMusicKey(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val stream = storage.download(key)
            call.respondOutputStream(ContentType.Application.Pdf) {
                stream.use { it.copyTo(this) }
            }
        }

        delete("/{id}/sheet-music") {
            requireRole("admin") ?: return@delete
            if (storage == null) {
                call.respond(HttpStatusCode.ServiceUnavailable, "Storage not configured")
                return@delete
            }
            val id = UUID.fromString(call.parameters["id"])
            val key = SongService.getSheetMusicKey(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            storage.delete(key)
            SongService.setSheetMusicKey(id, null)
            call.respond(HttpStatusCode.OK)
        }
    }
}
