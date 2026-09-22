package service

import auth.JwtConfig
import model.Users
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object AuthService {

    fun authenticate(username: String, password: String, jwtSecret: String, jwtIssuer: String, jwtAudience: String): String? {
        val user = transaction {
            Users.selectAll()
                .where { Users.username eq username }
                .singleOrNull()
        } ?: return null

        if (!BCrypt.checkpw(password, user[Users.passwordHash])) return null

        return JwtConfig.generateToken(
            userId = user[Users.id].toString(),
            role = user[Users.role],
            secret = jwtSecret,
            issuer = jwtIssuer,
            audience = jwtAudience,
        )
    }

    fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
}
