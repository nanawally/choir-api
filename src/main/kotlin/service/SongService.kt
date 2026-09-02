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

data class SongDTO(val id: UUID, val name: String)

object SongService {

    fun list(): List<SongDTO> = transaction {
        Songs.selectAll()
            .map { SongDTO(it[Songs.id], it[Songs.name]) }
    }

    fun create(name: String): SongDTO = transaction {
        val id = Songs.insert {
            it[Songs.name] = name
        } get Songs.id

        SongDTO(id, name)
    }

    fun rename(id: UUID, newName: String): Boolean = transaction {
        Songs.update({ Songs.id eq id }) {
            it[Songs.name] = newName
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
