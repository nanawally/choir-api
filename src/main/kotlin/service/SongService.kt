package service

import model.ConcertSongs
import model.HiddenChorists
import model.SongFormations
import model.Songs
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

data class SongDTO(
    val id: UUID,
    val name: String,
    val composer: String?,
    val arranger: String?,
    val delning: String?,
    val languages: String?,
    val length: String?,
    val accompanied: Boolean?,
    val instrument: String?,
    val year: Int?,
    val collectionName: String?,
    val soloists: String?,
    val hasSheetMusic: Boolean,
)

object SongService {

    private fun rowToDTO(row: org.jetbrains.exposed.sql.ResultRow) = SongDTO(
        id = row[Songs.id],
        name = row[Songs.name],
        composer = row[Songs.composer],
        arranger = row[Songs.arranger],
        delning = row[Songs.delning],
        languages = row[Songs.languages],
        length = row[Songs.length],
        accompanied = row[Songs.accompanied],
        instrument = row[Songs.instrument],
        year = row[Songs.year],
        collectionName = row[Songs.collectionName],
        soloists = row[Songs.soloists],
        hasSheetMusic = row[Songs.hasSheetMusic],
    )

    fun list(): List<SongDTO> = transaction {
        Songs.selectAll().map(::rowToDTO)
    }

    fun create(name: String): SongDTO = transaction {
        val id = Songs.insert {
            it[Songs.name] = name
        } get Songs.id

        SongDTO(id, name, null, null, null, null, null, null, null, null, null, null, false)
    }

    fun update(id: UUID, name: String, composer: String?, arranger: String?, delning: String?,
               languages: String?, length: String?, accompanied: Boolean?, instrument: String?,
               year: Int?, collectionName: String?, soloists: String?, hasSheetMusic: Boolean): Boolean = transaction {
        Songs.update({ Songs.id eq id }) {
            it[Songs.name] = name
            it[Songs.composer] = composer
            it[Songs.arranger] = arranger
            it[Songs.delning] = delning
            it[Songs.languages] = languages
            it[Songs.length] = length
            it[Songs.accompanied] = accompanied
            it[Songs.instrument] = instrument
            it[Songs.year] = year
            it[Songs.collectionName] = collectionName
            it[Songs.soloists] = soloists
            it[Songs.hasSheetMusic] = hasSheetMusic
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        val songIds = ConcertSongs.selectAll()
            .where { ConcertSongs.songId eq id }
            .map { it[ConcertSongs.id] }

        songIds.forEach { sId ->
            HiddenChorists.deleteWhere { HiddenChorists.concertSongId eq sId }
            SongFormations.deleteWhere { SongFormations.concertSongId eq sId }
        }
        ConcertSongs.deleteWhere { ConcertSongs.songId eq id }
        Songs.deleteWhere { Songs.id eq id } > 0
    }
}
