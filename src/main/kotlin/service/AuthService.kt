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

        val hash = user[Users.passwordHash] ?: return null // pending account, cannot log in
        if (!BCrypt.checkpw(password, hash)) return null

        return JwtConfig.generateToken(
            userId = user[Users.id].toString(),
            role = user[Users.role],
            choirId = user[Users.choirId].toString(),
            secret = jwtSecret,
            issuer = jwtIssuer,
            audience = jwtAudience,
        )
    }

    fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
}
