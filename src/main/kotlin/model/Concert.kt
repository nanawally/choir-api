package model

import org.jetbrains.exposed.sql.Table

object Concerts : Table("concerts") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)
    val date = varchar("date", 10).nullable()
    val imageUrl = varchar("image_url", 1024).nullable()

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
    val composer = varchar("composer", 255).nullable()
    val arranger = varchar("arranger", 255).nullable()
    val delning = varchar("delning", 255).nullable()
    val languages = varchar("languages", 500).nullable()  // comma-separated
    val length = varchar("length", 10).nullable()          // "MM:SS" or "MM"
    val accompanied = bool("accompanied").nullable()
    val instrument = varchar("instrument", 255).nullable()
    val year = integer("year").nullable()
    val collectionName = varchar("collection_name", 255).nullable()
    val soloists = varchar("soloists", 500).nullable()     // TBD: may become boolean
    val hasSheetMusic = bool("has_sheet_music").default(false)

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
