package service

import model.ConcertSongs
import model.HiddenChorists
import model.SongAudioFiles
import model.SongFormations
import model.SongLinks
import model.Songs
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import model.Concerts
import java.util.*

data class SongDTO(
    val id: UUID,
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
    val sheetMusicKey: String?,
    val lyrics: String?,
)

object SongService {

    private fun rowToDTO(row: org.jetbrains.exposed.sql.ResultRow) = SongDTO(
        id = row[Songs.id],
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
        sheetMusicKey = row[Songs.sheetMusicKey],
        lyrics = row[Songs.lyrics],
    )

    fun list(): List<SongDTO> = transaction {
        Songs.selectAll().map(::rowToDTO)
    }

    fun create(name: String): SongDTO = transaction {
        val id = Songs.insert {
            it[Songs.name] = name
        } get Songs.id

        SongDTO(id, name, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null)
    }

    fun update(id: UUID, name: String, composer: String?, arranger: String?, lyricist: String?, delning: String?,
               languages: String?, length: String?, accompanied: Boolean?, instrument: String?,
               year: Int?, collectionName: String?, hasSoloists: Boolean?, soloistNames: String?, hasSheetMusic: Boolean,
               lyrics: String?): Boolean = transaction {
        Songs.update({ Songs.id eq id }) {
            it[Songs.name] = name
            it[Songs.composer] = composer
            it[Songs.arranger] = arranger
            it[Songs.lyricist] = lyricist
            it[Songs.delning] = delning
            it[Songs.languages] = languages
            it[Songs.length] = length
            it[Songs.accompanied] = accompanied
            it[Songs.instrument] = instrument
            it[Songs.year] = year
            it[Songs.collectionName] = collectionName
            it[Songs.hasSoloists] = hasSoloists
            it[Songs.soloistNames] = soloistNames
            it[Songs.hasSheetMusic] = hasSheetMusic
            it[Songs.lyrics] = lyrics
        } > 0
    }

    fun setSheetMusicKey(id: UUID, key: String?): Boolean = transaction {
        Songs.update({ Songs.id eq id }) {
            it[sheetMusicKey] = key
            it[hasSheetMusic] = key != null
        } > 0
    }

    fun getSheetMusicKey(id: UUID): String? = transaction {
        Songs.selectAll().where { Songs.id eq id }
            .singleOrNull()?.get(Songs.sheetMusicKey)
    }

    fun listConcertsForSong(songId: UUID): List<Pair<UUID, String>> = transaction {
        (ConcertSongs innerJoin Concerts)
            .selectAll()
            .where { ConcertSongs.songId eq songId }
            .map { it[Concerts.id] to it[Concerts.name] }
            .distinctBy { it.first }
    }

    // Audio files
    data class AudioFileDTO(val id: UUID, val songId: UUID, val voicePartId: UUID?, val storageKey: String, val fileName: String)

    fun listAudioFiles(songId: UUID): List<AudioFileDTO> = transaction {
        SongAudioFiles.selectAll().where { SongAudioFiles.songId eq songId }
            .map { AudioFileDTO(it[SongAudioFiles.id], it[SongAudioFiles.songId], it[SongAudioFiles.voicePartId], it[SongAudioFiles.storageKey], it[SongAudioFiles.fileName]) }
    }

    fun addAudioFile(songId: UUID, voicePartId: UUID?, storageKey: String, fileName: String): AudioFileDTO = transaction {
        val id = SongAudioFiles.insert {
            it[SongAudioFiles.songId] = songId
            it[SongAudioFiles.voicePartId] = voicePartId
            it[SongAudioFiles.storageKey] = storageKey
            it[SongAudioFiles.fileName] = fileName
        } get SongAudioFiles.id
        AudioFileDTO(id, songId, voicePartId, storageKey, fileName)
    }

    fun getAudioFile(id: UUID): AudioFileDTO? = transaction {
        SongAudioFiles.selectAll().where { SongAudioFiles.id eq id }
            .firstOrNull()?.let { AudioFileDTO(it[SongAudioFiles.id], it[SongAudioFiles.songId], it[SongAudioFiles.voicePartId], it[SongAudioFiles.storageKey], it[SongAudioFiles.fileName]) }
    }

    fun deleteAudioFile(id: UUID): Boolean = transaction {
        SongAudioFiles.deleteWhere { SongAudioFiles.id eq id } > 0
    }

    // Links
    data class LinkDTO(val id: UUID, val songId: UUID, val url: String, val label: String?)

    fun listLinks(songId: UUID): List<LinkDTO> = transaction {
        SongLinks.selectAll().where { SongLinks.songId eq songId }
            .map { LinkDTO(it[SongLinks.id], it[SongLinks.songId], it[SongLinks.url], it[SongLinks.label]) }
    }

    fun addLink(songId: UUID, url: String, label: String?): LinkDTO = transaction {
        val id = SongLinks.insert {
            it[SongLinks.songId] = songId
            it[SongLinks.url] = url
            it[SongLinks.label] = label
        } get SongLinks.id
        LinkDTO(id, songId, url, label)
    }

    fun deleteLink(id: UUID): Boolean = transaction {
        SongLinks.deleteWhere { SongLinks.id eq id } > 0
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
        SongAudioFiles.deleteWhere { SongAudioFiles.songId eq id }
        SongLinks.deleteWhere { SongLinks.songId eq id }
        Songs.deleteWhere { Songs.id eq id } > 0
    }
}
