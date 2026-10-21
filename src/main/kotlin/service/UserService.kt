package service

import model.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.Normalizer
import java.time.Instant
import java.util.*

data class UserDTO(
    val id: UUID,
    val username: String,
    val choristId: UUID?,
    val role: String,
    val email: String?,
    val isArchived: Boolean,
    val isPending: Boolean,
)

data class UserSetupInfo(val userId: UUID, val username: String)

object UserService {

    fun generateUsername(firstName: String, lastName: String, choirId: UUID): String {
        fun normalize(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("[^a-zA-Z]"), "").lowercase()
        val base = normalize(firstName).take(3) + normalize(lastName).take(3)
        val existing = transaction {
            Users.selectAll().where { Users.choirId eq choirId }.map { it[Users.username] }.toSet()
        }
        if (base !in existing) return base
        var n = 2
        while ("$base$n" in existing) n++
        return "$base$n"
    }

    fun createForChorist(choristId: UUID, choirId: UUID, firstName: String, lastName: String): Pair<String, String> = transaction {
        val username = generateUsername(firstName, lastName, choirId)
        val token = newToken()
        val expiresAt = Instant.now().plusSeconds(72 * 3600).toString()
        Users.insert {
            it[Users.choirId] = choirId
            it[Users.choristId] = choristId
            it[Users.username] = username
            it[Users.role] = "user"
            it[Users.setupToken] = token
            it[Users.setupTokenExpiresAt] = expiresAt
        }
        Pair(username, token)
    }

    fun createNonChorist(choirId: UUID, username: String, role: String = "user"): String = transaction {
        val token = newToken()
        val expiresAt = Instant.now().plusSeconds(72 * 3600).toString()
        Users.insert {
            it[Users.choirId] = choirId
            it[Users.username] = username
            it[Users.role] = role
            it[Users.setupToken] = token
            it[Users.setupTokenExpiresAt] = expiresAt
        }
        token
    }

    fun getByChoristId(choristId: UUID): UserDTO? = transaction {
        Users.selectAll().where { Users.choristId eq choristId }.singleOrNull()?.let { toDTO(it) }
    }

    fun lookupBySetupToken(token: String): UserSetupInfo? = transaction {
        val row = Users.selectAll().where { Users.setupToken eq token }.singleOrNull() ?: return@transaction null
        val expiresAt = row[Users.setupTokenExpiresAt] ?: return@transaction null
        if (Instant.now().isAfter(Instant.parse(expiresAt))) return@transaction null
        UserSetupInfo(row[Users.id], row[Users.username])
    }

    fun claimAccount(token: String, password: String): Boolean = transaction {
        val row = Users.selectAll().where { Users.setupToken eq token }.singleOrNull() ?: return@transaction false
        val expiresAt = row[Users.setupTokenExpiresAt] ?: return@transaction false
        if (Instant.now().isAfter(Instant.parse(expiresAt))) return@transaction false
        val hash = AuthService.hashPassword(password)
        Users.update({ Users.id eq row[Users.id] }) {
            it[Users.passwordHash] = hash
            it[Users.setupToken] = null
            it[Users.setupTokenExpiresAt] = null
        }
        true
    }

    fun resetAccount(userId: UUID, choirId: UUID): String? = transaction {
        val exists = Users.selectAll()
            .where { (Users.id eq userId) and (Users.choirId eq choirId) }
            .singleOrNull() ?: return@transaction null
        val token = newToken()
        val expiresAt = Instant.now().plusSeconds(72 * 3600).toString()
        Users.update({ Users.id eq exists[Users.id] }) {
            it[Users.passwordHash] = null
            it[Users.setupToken] = token
            it[Users.setupTokenExpiresAt] = expiresAt
        }
        token
    }

    fun list(choirId: UUID): List<UserDTO> = transaction {
        Users.selectAll().where { Users.choirId eq choirId }.map { toDTO(it) }
    }

    fun setRole(userId: UUID, choirId: UUID, role: String): Boolean = transaction {
        Users.update({ (Users.id eq userId) and (Users.choirId eq choirId) }) {
            it[Users.role] = role
        } > 0
    }

    fun archive(userId: UUID, choirId: UUID): Boolean = transaction {
        Users.update({ (Users.id eq userId) and (Users.choirId eq choirId) }) {
            it[Users.isArchived] = true
        } > 0
    }

    fun unarchive(userId: UUID, choirId: UUID): Boolean = transaction {
        Users.update({ (Users.id eq userId) and (Users.choirId eq choirId) }) {
            it[Users.isArchived] = false
        } > 0
    }

    fun isSuperUser(userId: UUID): Boolean = transaction {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.isSuperUser) ?: false
    }

    fun getById(userId: UUID): UserDTO? = transaction {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()?.let { toDTO(it) }
    }

    fun delete(userId: UUID, choirId: UUID): Boolean = transaction {
        val exists = Users.selectAll()
            .where { (Users.id eq userId) and (Users.choirId eq choirId) }
            .singleOrNull() != null
        if (!exists) return@transaction false
        Users.deleteWhere { (Users.id eq userId) and (Users.choirId eq choirId) } > 0
    }

    private fun toDTO(row: ResultRow) = UserDTO(
        id = row[Users.id],
        username = row[Users.username],
        choristId = row[Users.choristId],
        role = row[Users.role],
        email = row[Users.email],
        isArchived = row[Users.isArchived],
        isPending = row[Users.passwordHash] == null,
    )

    private fun newToken(): String = UUID.randomUUID().toString().replace("-", "")
}
