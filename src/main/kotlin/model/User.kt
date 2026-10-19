package model

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = uuid("id").autoGenerate()
    val choirId = uuid("choir_id").references(Choirs.id)
    val choristId = uuid("chorist_id").references(Chorists.id).nullable()
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val role = varchar("role", 50) // "admin" or "user" — transitional until permissions phase
    val isSuperUser = bool("is_super_user").default(false)
    val email = varchar("email", 255).nullable()
    val setupToken = varchar("setup_token", 64).nullable()
    val setupTokenExpiresAt = varchar("setup_token_expires_at", 50).nullable()
    val isArchived = bool("is_archived").default(false)

    override val primaryKey = PrimaryKey(id)
}
