package model

import org.jetbrains.exposed.sql.Table

object Chorists : Table("chorists") {
    val id = uuid("id").autoGenerate()
    val firstName = varchar("first_name", 255).default("")
    val lastName = varchar("last_name", 255).default("")
    val isSectionLeader = bool("is_section_leader").default(false)
    val isArchived = bool("is_archived").default(false)
    val notes = text("notes").nullable()
    val choirId = uuid("choir_id").references(Choirs.id)

    override val primaryKey = PrimaryKey(id)
}
