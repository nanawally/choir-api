package routes

import auth.requireRole
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.utils.io.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.SongbookService
import service.StorageService
import java.util.*

@Serializable
data class CreateSongbookRequest(val name: String, val date: String? = null, val isPinned: Boolean = false)

@Serializable
data class UpdateSongbookRequest(val name: String, val date: String? = null, val isPinned: Boolean = false)

@Serializable
data class DuplicateSongbookRequest(val name: String)

@Serializable
data class SongbookResponse(val id: String, val name: String, val date: String? = null, val imageUrl: String? = null, val isPinned: Boolean)

@Serializable
data class AddSongToSongbookRequest(val songId: String)

@Serializable
data class ReorderSongbookSongsRequest(val songIds: List<String>)

@Serializable
data class SongbookSongResponse(
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
    val hasSheetMusic: Boolean,
    val hasSheetMusicFile: Boolean,
    val lyrics: String? = null,
    val sortOrder: Int
)

fun Route.songbookRoutes(storage: StorageService?) {
    route("/songbooks") {
        get {
            val songbooks = SongbookService.list().map {
                SongbookResponse(it.id.toString(), it.name, it.date, it.imageUrl, it.isPinned)
            }
            call.respond(songbooks)
        }

        post {
            requireRole("admin") ?: return@post
            val req = call.receive<CreateSongbookRequest>()
            val songbook = SongbookService.create(req.name, req.date, req.isPinned)
            call.respond(
                HttpStatusCode.Created,
                SongbookResponse(songbook.id.toString(), songbook.name, songbook.date, songbook.imageUrl, songbook.isPinned)
            )
        }

        put("/{id}") {
            requireRole("admin") ?: return@put
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<UpdateSongbookRequest>()
            respondOkOrNotFound(SongbookService.update(id, req.name, req.date, req.isPinned))
        }

        delete("/{id}") {
            requireRole("admin") ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            respondOkOrNotFound(SongbookService.delete(id))
        }

        post("/{id}/duplicate") {
            requireRole("admin") ?: return@post
            val id = UUID.fromString(call.parameters["id"])
            val req = call.receive<DuplicateSongbookRequest>()
            val songbook = SongbookService.duplicate(id, req.name)
            if (songbook != null) {
                call.respond(HttpStatusCode.Created, SongbookResponse(songbook.id.toString(), songbook.name, songbook.date, songbook.imageUrl, songbook.isPinned))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/{id}/image") {
            requireRole("admin") ?: return@post
            val s = requireStorage(storage) ?: return@post
            val id = UUID.fromString(call.parameters["id"])
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var contentType = "image/jpeg"

            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "file") {
                    fileBytes = part.provider().toByteArray()
                    contentType = part.contentType?.toString() ?: "image/jpeg"
                }
                part.dispose()
            }

            val bytes = fileBytes
            if (bytes == null) {
                call.respond(HttpStatusCode.BadRequest, "No file provided")
                return@post
            }

            if (contentType !in listOf("image/jpeg", "image/png", "image/webp")) {
                call.respond(HttpStatusCode.BadRequest, "Unsupported image type: $contentType. Use JPEG, PNG, or WebP.")
                return@post
            }

            val extension = if (contentType.contains("png")) "png" else "jpg"
            val key = "songbook-images/$id.$extension"
            s.upload(key, bytes, contentType)
            SongbookService.setImageUrl(id, key)

            call.respond(HttpStatusCode.OK, mapOf("key" to key))
        }

        get("/{id}/image") {
            val s = requireStorage(storage) ?: return@get
            val id = UUID.fromString(call.parameters["id"])
            val key = SongbookService.getImageUrl(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val ct = if (key.endsWith(".png")) ContentType.Image.PNG else ContentType.Image.JPEG
            val stream = s.download(key)
            call.respondOutputStream(ct) {
                stream.use { it.copyTo(this) }
            }
        }

        delete("/{id}/image") {
            requireRole("admin") ?: return@delete
            val s = requireStorage(storage) ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            val key = SongbookService.getImageUrl(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            s.delete(key)
            SongbookService.setImageUrl(id, null)
            call.respond(HttpStatusCode.OK)
        }

        route("/{id}/songs") {
            get {
                val id = UUID.fromString(call.parameters["id"])
                val songs = SongbookService.listSongs(id).map {
                    SongbookSongResponse(
                        id = it.id,
                        name = it.name,
                        composer = it.composer,
                        arranger = it.arranger,
                        lyricist = it.lyricist,
                        delning = it.delning,
                        languages = it.languages,
                        length = it.length,
                        accompanied = it.accompanied,
                        instrument = it.instrument,
                        year = it.year,
                        collectionName = it.collectionName,
                        hasSoloists = it.hasSoloists,
                        soloistNames = it.soloistNames,
                        hasSheetMusic = it.hasSheetMusic,
                        hasSheetMusicFile = it.hasSheetMusicFile,
                        lyrics = it.lyrics,
                        sortOrder = it.sortOrder
                    )
                }
                call.respond(songs)
            }

            post {
                requireRole("admin") ?: return@post
                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<AddSongToSongbookRequest>()
                val added = SongbookService.addSong(id, UUID.fromString(req.songId))
                if (added) call.respond(HttpStatusCode.Created) else call.respond(HttpStatusCode.Conflict)
            }

            delete("/{songId}") {
                requireRole("admin") ?: return@delete
                val id = UUID.fromString(call.parameters["id"])
                val songId = UUID.fromString(call.parameters["songId"])
                respondOkOrNotFound(SongbookService.removeSong(id, songId))
            }

            put("/order") {
                requireRole("admin") ?: return@put
                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<ReorderSongbookSongsRequest>()
                SongbookService.reorderSongs(id, req.songIds.map { UUID.fromString(it) })
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}