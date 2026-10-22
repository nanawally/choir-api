package service

import model.ConcertSongs
import model.SongbookSongs
import model.Songbooks
import model.Songs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class SongbookDTO(
    val id: UUID,
    val name: String,
    val date: String?,
    val imageUrl: String?,
    val isPinned: Boolean,
    val isHidden: Boolean = false,
)

data class SongbookSongDTO(
    val id: String,
    val name: String,
    val composer: String?,
    val arranger: String?,
    val lyricist: String?,
    val delning: String?,
    val languages: String?,
    val length: String?,
    val accompanied: Boolean?,
    val instrument: String?,
    val year: Int?,
    val collectionName: String?,
    val hasSoloists: Boolean?,
    val soloistNames: String?,
    val hasSheetMusic: Boolean,
    val hasSheetMusicFile: Boolean,
    val lyrics: String?,
    val sortOrder: Int
)

object SongbookService {

    fun list(choirId: UUID, canViewHidden: Boolean = false): List<SongbookDTO> = transaction {
        Songbooks.selectAll()
            .where {
                (Songbooks.choirId eq choirId) and
                if (canViewHidden) Op.TRUE else (Songbooks.isHidden eq false)
            }
            .map { row ->
                SongbookDTO(row[Songbooks.id], row[Songbooks.name], row[Songbooks.date], row[Songbooks.imageUrl], row[Songbooks.isPinned], row[Songbooks.isHidden])
            }
    }

    fun setHidden(id: UUID, choirId: UUID, isHidden: Boolean): Boolean = transaction {
        Songbooks.update({ (Songbooks.id eq id) and (Songbooks.choirId eq choirId) }) {
            it[Songbooks.isHidden] = isHidden
        } > 0
    }

    fun create(choirId: UUID, name: String, date: String?, isPinned: Boolean): SongbookDTO = transaction {
        val id = Songbooks.insert {
            it[Songbooks.choirId] = choirId
            it[Songbooks.name] = name
            it[Songbooks.date] = date
            it[Songbooks.isPinned] = isPinned
        } get Songbooks.id
        SongbookDTO(id, name, date, null, isPinned)
    }

    fun update(id: UUID, choirId: UUID, name: String, date: String?, isPinned: Boolean): Boolean = transaction {
        Songbooks.update({ (Songbooks.id eq id) and (Songbooks.choirId eq choirId) }) {
            it[Songbooks.name] = name
            it[Songbooks.date] = date
            it[Songbooks.isPinned] = isPinned
        } > 0
    }

    fun delete(id: UUID, choirId: UUID): Boolean = transaction {
        SongbookSongs.deleteWhere { SongbookSongs.songbookId eq id }
        Songbooks.deleteWhere { (Songbooks.id eq id) and (Songbooks.choirId eq choirId) } > 0
    }

    fun getImageUrl(id: UUID): String? = transaction {
        Songbooks.selectAll().where { Songbooks.id eq id }.firstOrNull()?.get(Songbooks.imageUrl)
    }

    fun setImageUrl(id: UUID, url: String?) = transaction {
        Songbooks.update({ Songbooks.id eq id }) { it[imageUrl] = url }
    }

    fun duplicate(id: UUID, choirId: UUID, newName: String): SongbookDTO? = transaction {
        val original = Songbooks.selectAll()
            .where { (Songbooks.id eq id) and (Songbooks.choirId eq choirId) }
            .firstOrNull() ?: return@transaction null
        val newId = Songbooks.insert {
            it[Songbooks.choirId] = choirId
            it[name] = newName
            it[date] = original[Songbooks.date]
            it[imageUrl] = original[Songbooks.imageUrl]
            it[isPinned] = false
        } get Songbooks.id
        SongbookSongs.selectAll().where { SongbookSongs.songbookId eq id }.forEach { ss ->
            SongbookSongs.insert {
                it[songbookId] = newId
                it[songId] = ss[SongbookSongs.songId]
                it[sortOrder] = ss[SongbookSongs.sortOrder]
            }
        }
        SongbookDTO(newId, newName, original[Songbooks.date], original[Songbooks.imageUrl], false)
    }

    fun createFromConcert(choirId: UUID, concertId: UUID, name: String, date: String?): SongbookDTO = transaction {
        val newId = Songbooks.insert {
            it[Songbooks.choirId] = choirId
            it[Songbooks.name] = name
            it[Songbooks.date] = date
            it[Songbooks.isPinned] = false
        } get Songbooks.id

        ConcertSongs.selectAll()
            .where { ConcertSongs.concertId eq concertId }
            .orderBy(ConcertSongs.sortOrder to SortOrder.ASC)
            .forEachIndexed { index, row ->
                SongbookSongs.insert {
                    it[SongbookSongs.songbookId] = newId
                    it[SongbookSongs.songId] = row[ConcertSongs.songId]
                    it[SongbookSongs.sortOrder] = index
                }
            }

        SongbookDTO(newId, name, date, null, false)
    }

    fun listSongs(songbookId: UUID): List<SongbookSongDTO> = transaction {
        (SongbookSongs innerJoin Songs)
            .selectAll()
            .where { SongbookSongs.songbookId eq songbookId }
            .orderBy(SongbookSongs.sortOrder to SortOrder.ASC)
            .map { row ->
                SongbookSongDTO(
                    id = row[Songs.id].toString(),
                    name = row[Songs.name],
                    composer = row[Songs.composer],
                    arranger = row[Songs.arranger],
                    lyricist = row[Songs.lyricist],
                    delning = row[Songs.delning],
                    languages = row[Songs.languages],
                    length = row[Songs.length],
                    accompanied = row[Songs.accompanied],
                    instrument = row[Songs.instrument],
                    year = row[Songs.year],
                    collectionName = row[Songs.collectionName],
                    hasSoloists = row[Songs.hasSoloists],
                    soloistNames = row[Songs.soloistNames],
                    hasSheetMusic = row[Songs.hasSheetMusic],
                    hasSheetMusicFile = row[Songs.sheetMusicKey] != null,
                    lyrics = row[Songs.lyrics],
                    sortOrder = row[SongbookSongs.sortOrder]
                )
            }
    }

    fun addSong(songbookId: UUID, songId: UUID): Boolean = transaction {
        val alreadyExists = SongbookSongs.selectAll()
            .where { (SongbookSongs.songbookId eq songbookId) and (SongbookSongs.songId eq songId) }
            .count() > 0
        if (alreadyExists) return@transaction false
        val maxOrder = SongbookSongs.selectAll()
            .where { SongbookSongs.songbookId eq songbookId }
            .maxOfOrNull { it[SongbookSongs.sortOrder] } ?: -1
        SongbookSongs.insert {
            it[SongbookSongs.songbookId] = songbookId
            it[SongbookSongs.songId] = songId
            it[SongbookSongs.sortOrder] = maxOrder + 1
        }
        true
    }

    fun removeSong(songbookId: UUID, songId: UUID): Boolean = transaction {
        SongbookSongs.deleteWhere {
            (SongbookSongs.songbookId eq songbookId) and (SongbookSongs.songId eq songId)
        } > 0
    }

    fun reorderSongs(songbookId: UUID, orderedSongIds: List<UUID>) = transaction {
        orderedSongIds.forEachIndexed { index, songId ->
            SongbookSongs.update({
                (SongbookSongs.songbookId eq songbookId) and (SongbookSongs.songId eq songId)
            }) {
                it[SongbookSongs.sortOrder] = index
            }
        }
    }
}
