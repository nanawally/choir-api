package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private const val TOKEN_VALIDITY_MS = 7L * 24 * 60 * 60 * 1000 // 7 days

    fun generateToken(
        userId: String,
        role: String,
        choirId: String,
        secret: String,
        issuer: String,
        audience: String,
    ): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withClaim("choirId", choirId)
            .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
            .sign(Algorithm.HMAC256(secret))
    }
}
