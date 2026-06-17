package model

import org.jetbrains.exposed.sql.Table

object Concerts : Table("concerts") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

object Formations : Table("formations") {
    val id = uuid("id").autoGenerate()
    val concertId = uuid("concert_id").references(Concerts.id)
    val name = varchar("name", 255)
    val sortOrder = integer("sort_order").default(0)

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
    val formationId = uuid("formation_id").references(Formations.id)
    val choristId = uuid("chorist_id").references(Chorists.id)

    override val primaryKey = PrimaryKey(id)
}