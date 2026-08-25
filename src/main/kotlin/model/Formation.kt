package model

import org.jetbrains.exposed.sql.Table

object Formations : Table("formations") {
    val id = uuid("id").autoGenerate()
    val concertId = uuid("concert_id").references(Concerts.id)
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

object Placements : Table("placements") {
    val id = uuid("id").autoGenerate()
    val formationId = uuid("formation_id").references(Formations.id)
    val choristId = uuid("chorist_id").references(Chorists.id)
    val gridX = integer("grid_x")
    val gridY = integer("grid_y")

    override val primaryKey = PrimaryKey(id)
}

object HiddenChorists : Table("hidden_chorists") {
    val id = uuid("id").autoGenerate()
    val songId = uuid("song_id").references(Songs.id)
    val choristId = uuid("chorist_id").references(Chorists.id)

    override val primaryKey = PrimaryKey(id)
}
