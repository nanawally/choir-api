package model

import org.jetbrains.exposed.sql.Table

object Chorists : Table("chorists") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)
    val isSectionLeader = bool("is_section_leader").default(false)
    val isArchived = bool("is_archived").default(false)

    override val primaryKey = PrimaryKey(id)
}
