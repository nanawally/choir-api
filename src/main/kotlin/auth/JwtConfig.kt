package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    fun generateToken(
        userId: String,
        role: String,
        secret: String,
        issuer: String,
        audience: String,
    ): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)) // 7 days
            .sign(Algorithm.HMAC256(secret))
    }
}
