package routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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