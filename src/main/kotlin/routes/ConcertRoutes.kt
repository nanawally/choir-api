package routes

import auth.extractChoirId
import auth.requirePermission
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import service.ConcertService
import service.SongbookService
import service.StorageService
import java.util.*

@Serializable
data class CreateConcertRequest(val name: String, val date: String? = null)

@Serializable
data class UpdateConcertRequest(val name: String, val date: String? = null)

@Serializable
data class DuplicateConcertRequest(val name: String)

@Serializable
data class CreateSongbookFromConcertRequest(val name: String, val date: String? = null)

@Serializable
data class ConcertResponse(val id: String, val name: String, val date: String? = null, val imageUrl: String? = null)

fun Route.concertRoutes(storage: StorageService?) {
    route("/concerts") {
        get {
            if (!requirePermission("concerts", "view")) return@get
            val choirId = extractChoirId()
            val concerts = ConcertService.list(choirId).map {
                ConcertResponse(it.id.toString(), it.name, it.date, it.imageUrl)
            }
            call.respond(concerts)
        }

        post {
            if (!requirePermission("concerts", "edit")) return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateConcertRequest>()
            val concert = ConcertService.create(choirId, req.name, req.date)
            call.respond(
                HttpStatusCode.Created,
                ConcertResponse(concert.id.toString(), concert.name, concert.date, concert.imageUrl)
            )
        }

        put("/{id}") {
            if (!requirePermission("concerts", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<UpdateConcertRequest>()
            respondOkOrNotFound(ConcertService.update(id, choirId, req.name, req.date))
        }

        delete("/{id}") {
            if (!requirePermission("concerts", "delete")) return@delete
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(ConcertService.delete(id, choirId))
        }

        post("/{id}/create-songbook") {
            if (!requirePermission("concerts", "view")) return@post
            if (!requirePermission("songbooks", "edit")) return@post
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<CreateSongbookFromConcertRequest>()
            val songbook = SongbookService.createFromConcert(choirId, id, req.name, req.date)
            call.respond(
                HttpStatusCode.Created,
                SongbookResponse(songbook.id.toString(), songbook.name, songbook.date, songbook.imageUrl, songbook.isPinned, songbook.isHidden)
            )
        }

        post("/{id}/duplicate") {
            if (!requirePermission("concerts", "edit")) return@post
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<DuplicateConcertRequest>()
            val concert = ConcertService.duplicate(id, choirId, req.name)
            if (concert != null) {
                call.respond(HttpStatusCode.Created, ConcertResponse(concert.id.toString(), concert.name, concert.date, concert.imageUrl))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/{id}/image") {
            if (!requirePermission("concerts", "edit")) return@post
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
            val key = "concert-images/$id.$extension"
            s.upload(key, bytes, contentType)
            ConcertService.setImageUrl(id, key)

            call.respond(HttpStatusCode.OK, mapOf("key" to key))
        }

        get("/{id}/image") {
            if (!requirePermission("concerts", "view")) return@get
            val s = requireStorage(storage) ?: return@get
            val id = UUID.fromString(call.parameters["id"])
            val key = ConcertService.getImageUrl(id)
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
            if (!requirePermission("concerts", "edit")) return@delete
            val s = requireStorage(storage) ?: return@delete
            val id = UUID.fromString(call.parameters["id"])
            val key = ConcertService.getImageUrl(id)
            if (key == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }
            s.delete(key)
            ConcertService.setImageUrl(id, null)
            call.respond(HttpStatusCode.OK)
        }
    }
}
