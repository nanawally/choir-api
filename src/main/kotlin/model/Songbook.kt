package model

import org.jetbrains.exposed.sql.Table

object Songbooks : Table("songbooks") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 255)
    val date = varchar("date", 10).nullable()
    val imageUrl = varchar("image_url", 1024).nullable()
    val isPinned = bool("is_pinned").default(false)

    override val primaryKey = PrimaryKey(id)
}

object SongbookSongs : Table("songbook_songs") {
    val id = uuid("id").autoGenerate()
    val songbookId = uuid("songbook_id").references(Songbooks.id)
    val songId = uuid("song_id").references(Songs.id)
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(id)
}