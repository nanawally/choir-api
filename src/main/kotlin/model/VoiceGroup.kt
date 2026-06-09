package model

import org.jetbrains.exposed.sql.Table

// E.g. "4-part harmony"
object VoiceGroups : Table("voice_groups") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

// E.g. "Sop 1", with colour and shape
object VoiceParts : Table("voice_parts") {
    val id = uuid("id").autoGenerate()
    val voiceGroupId = uuid("voice_group_id").references(VoiceGroups.id)
    val name = varchar("name", 255)
    val color = varchar("color", 30)
    val shape = varchar("shape", 30)

    override val primaryKey = PrimaryKey(id)
}

// Linking chorists to parts
object VoiceAssignments : Table("voice_assignments") {
    val id = uuid("id").autoGenerate()
    val choristId = uuid("chorist_id").references(Chorists.id)
    val voicePartId = uuid("voice_part_id").references(VoiceParts.id)

    override val primaryKey = PrimaryKey(id)
}