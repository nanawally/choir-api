package routes

import auth.extractUserId
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import service.PermissionService
import service.StorageService

suspend fun RoutingContext.respondOkOrNotFound(found: Boolean) {
    if (found) call.respond(HttpStatusCode.OK)
    else call.respond(HttpStatusCode.NotFound)
}

suspend fun RoutingContext.requireStorage(storage: StorageService?): StorageService? {
    if (storage == null) {
        call.respond(HttpStatusCode.ServiceUnavailable, "Storage not configured")
        return null
    }
    return storage
}

suspend fun RoutingContext.requireSongViewPermission(): Boolean {
    val uid = extractUserId()
    if (!PermissionService.check(uid, "songs", "view") && !PermissionService.check(uid, "songbooks", "view")) {
        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        return false
    }
    return true
}

data class UploadedImage(val bytes: ByteArray, val contentType: String)

suspend fun RoutingContext.receiveImageUpload(): UploadedImage? {
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
        return null
    }

    if (contentType !in listOf("image/jpeg", "image/png", "image/webp")) {
        call.respond(HttpStatusCode.BadRequest, "Unsupported image type: $contentType. Use JPEG, PNG, or WebP.")
        return null
    }

    return UploadedImage(bytes, contentType)
}

fun uploadImageKey(prefix: String, id: java.util.UUID, contentType: String): String {
    val extension = if (contentType.contains("png")) "png" else "jpg"
    return "$prefix/$id.$extension"
}