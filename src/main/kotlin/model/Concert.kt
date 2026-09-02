package model

import org.jetbrains.exposed.sql.Table

object Concerts : Table("concerts") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

object ConcertChorists : Table("concert_chorists") {
    val id = uuid("id").autoGenerate()
    val concertId = uuid("concert_id").references(Concerts.id)
    val choristId = uuid("chorist_id").references(Chorists.id)

    override val primaryKey = PrimaryKey(id)
}

object Songs : Table("songs") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

object ConcertSongs : Table("concert_songs") {
    val id = uuid("id").autoGenerate()
    val concertId = uuid("concert_id").references(Concerts.id)
    val songId = uuid("song_id").references(Songs.id)
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(id)
}

object SongFormations : Table("song_formations") {
    val id = uuid("id").autoGenerate()
    val concertSongId = uuid("concert_song_id").references(ConcertSongs.id)
    val formationId = uuid("formation_id").references(Formations.id)
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(id)
}
