package model

import org.jetbrains.exposed.sql.Table

object Chorists : Table("chorists") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}
