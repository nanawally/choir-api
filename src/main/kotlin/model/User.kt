package model

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = uuid("id").autoGenerate()
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50) // "admin" or "user"
    val choirId = uuid("choir_id").references(Choirs.id)

    override val primaryKey = PrimaryKey(id)
}
