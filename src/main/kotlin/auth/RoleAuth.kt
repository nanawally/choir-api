package auth

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.PermissionService
import java.util.*

suspend fun RoutingContext.requireRole(vararg roles: String): JWTPrincipal? {
    val principal = call.principal<JWTPrincipal>()
    if (principal == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
        return null
    }

    val role = principal.payload.getClaim("role").asString()
    if (role !in roles) {
        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        return null
    }

    return principal
}

/** Returns false and responds 403 if the user lacks the given permission; true otherwise. */
suspend fun RoutingContext.requirePermission(feature: String, action: String): Boolean {
    val principal = call.principal<JWTPrincipal>()
    if (principal == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
        return false
    }
    val userId = UUID.fromString(principal.payload.getClaim("userId").asString())
    if (!PermissionService.check(userId, feature, action)) {
        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        return false
    }
    return true
}

fun RoutingContext.extractChoirId(): UUID {
    val principal = call.principal<JWTPrincipal>()!!
    return UUID.fromString(principal.payload.getClaim("choirId").asString())
}

fun RoutingContext.extractUserId(): UUID {
    val principal = call.principal<JWTPrincipal>()!!
    return UUID.fromString(principal.payload.getClaim("userId").asString())
}
