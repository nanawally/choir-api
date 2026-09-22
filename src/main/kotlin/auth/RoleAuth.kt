package auth

import io.ktor.http.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
