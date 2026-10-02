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
            respondOkOrNotFound(SongService.update(id, req.name, req.composer, req.arranger, req.lyricist, req.delning,
                req.languages, req.length, req.accompanied, req.instrument,
                req.year, req.collectionName, req.hasSoloists, req.soloistNames, req.hasSheetMusic,
                req.lyrics))
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            // Delete sheet music file from storage if it exists
            val key = SongService.getSheetMusicKey(id)
            if (key != null && storage != null) {
                storage.delete(key)
            }
            respondOkOrNotFound(SongService.delete(id))
        }

        post("/{id}/sheet-music") {
            requireRole("admin") ?: return@post
            val s = requireStorage(storage) ?: return@post
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
                s.delete(oldKey)
            }

            val key = "sheet-music/$id.pdf"
            s.upload(key, compressed, "application/pdf")
            SongService.setSheetMusicKey(id, key)

            call.respond(HttpStatusCode.OK, mapOf("key" to key, "originalSize" to bytes.size, "compressedSize" to compressed.size))
        }

        get("/{id}/sheet-music") {
            val s = requireStorage(storage) ?: return@get
            val id = UUID.fromString(call.parameters["id"])
            val key = SongService.getSheetMusicKey(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val stream = s.download(key)
            call.respondOutputStream(ContentType.Application.Pdf) {
                stream.use { it.copyTo(this) }
            }
        }

        delete("/{id}/sheet-music") {
            requireRole("admin") ?: return@delete
            val s = requireStorage(storage) ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            val key = SongService.getSheetMusicKey(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            s.delete(key)
            SongService.setSheetMusicKey(id, null)
            call.respond(HttpStatusCode.OK)
        }
        // Audio files (stämfiler)
        get("/{id}/audio-files") {
            val id = UUID.fromString(call.parameters["id"])
            val files = SongService.listAudioFiles(id).map {
                mapOf("id" to it.id.toString(), "voicePartId" to it.voicePartId?.toString(), "fileName" to it.fileName)
            }
            call.respond(files)
        }

        post("/{id}/audio-files") {
            requireRole("admin") ?: return@post
            val s = requireStorage(storage) ?: return@post
            val songId = UUID.fromString(call.parameters["id"])
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var fileName = "audio.mp3"
            var voicePartId: String? = null
            var contentType = "audio/mpeg"

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> if (part.name == "file") {
                        fileBytes = part.provider().toByteArray()
                        fileName = part.originalFileName ?: "audio.mp3"
                        contentType = part.contentType?.toString() ?: "audio/mpeg"
                    }
                    is PartData.FormItem -> if (part.name == "voicePartId") {
                        voicePartId = part.value.takeIf { it.isNotBlank() }
                    }
                    else -> {}
                }
                part.dispose()
            }

            val bytes = fileBytes
            if (bytes == null) {
                call.respond(HttpStatusCode.BadRequest, "No file provided")
                return@post
            }

            val vpId = voicePartId?.let { UUID.fromString(it) }
            val key = "stamfiler/$songId/${UUID.randomUUID()}"
            s.upload(key, bytes, contentType)
            val dto = SongService.addAudioFile(songId, vpId, key, fileName)
            call.respond(HttpStatusCode.Created, mapOf("id" to dto.id.toString(), "voicePartId" to dto.voicePartId?.toString(), "fileName" to dto.fileName))
        }

        delete("/audio-files/{fileId}") {
            requireRole("admin") ?: return@delete
            val s = requireStorage(storage) ?: return@delete
            val fileId = UUID.fromString(call.parameters["fileId"])
            val file = SongService.getAudioFile(fileId)
            if (file == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            s.delete(file.storageKey)
            SongService.deleteAudioFile(fileId)
            call.respond(HttpStatusCode.OK)
        }

        get("/audio-files/{fileId}/stream") {
            val s = requireStorage(storage) ?: return@get
            val fileId = UUID.fromString(call.parameters["fileId"])
            val file = SongService.getAudioFile(fileId)
            if (file == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val stream = s.download(file.storageKey)
            val ct = if (file.fileName.endsWith(".wav")) ContentType.Audio.Any
                     else ContentType("audio", "mpeg")
            call.respondOutputStream(ct) {
                stream.use { it.copyTo(this) }
            }
        }

        // Song links (YouTube, Spotify, etc.)
        get("/{id}/links") {
            val id = UUID.fromString(call.parameters["id"])
            val links = SongService.listLinks(id).map {
                mapOf("id" to it.id.toString(), "url" to it.url, "label" to it.label)
            }
            call.respond(links)
        }

        post("/{id}/links") {
            requireRole("admin") ?: return@post
            val songId = UUID.fromString(call.parameters["id"])
            @Serializable data class AddLinkRequest(val url: String, val label: String? = null)
            val req = call.receive<AddLinkRequest>()
            val dto = SongService.addLink(songId, req.url, req.label)
            call.respond(HttpStatusCode.Created, mapOf("id" to dto.id.toString(), "url" to dto.url, "label" to dto.label))
        }

        delete("/links/{linkId}") {
            requireRole("admin") ?: return@delete
            val linkId = UUID.fromString(call.parameters["linkId"])
            respondOkOrNotFound(SongService.deleteLink(linkId))
        }
    }
}
