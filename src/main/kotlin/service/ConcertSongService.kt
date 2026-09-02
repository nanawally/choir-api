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

data class ConcertSongDTO(val id: UUID, val name: String, val sortOrder: Int)

object ConcertSongService {

    fun listByConcert(concertId: UUID): List<ConcertSongDTO> = transaction {
        ConcertSongs.selectAll()
            .where { ConcertSongs.concertId eq concertId }
            .orderBy(ConcertSongs.sortOrder)
            .map {
                val song = Songs.selectAll()
                    .where { Songs.id eq it[ConcertSongs.songId] }
                    .first()
                ConcertSongDTO(it[ConcertSongs.id], song[Songs.name], it[ConcertSongs.sortOrder])
            }
    }

    fun addToConcert(concertId: UUID, songId: UUID): ConcertSongDTO = transaction {
        val max = ConcertSongs.selectAll()
            .where { ConcertSongs.concertId eq concertId }
            .maxOfOrNull { it[ConcertSongs.sortOrder] } ?: 0

        val id = ConcertSongs.insert {
            it[ConcertSongs.concertId] = concertId
            it[ConcertSongs.songId] = songId
            it[ConcertSongs.sortOrder] = max + 1
        } get ConcertSongs.id

        val name = Songs.selectAll()
            .where { Songs.id eq songId }
            .map { it[Songs.name] }
            .first()

        ConcertSongDTO(id, name, max + 1)
    }

    fun removeFromConcert(concertSongId: UUID): Boolean = transaction {
        HiddenChorists.deleteWhere { HiddenChorists.concertSongId eq concertSongId }
        SongFormations.deleteWhere { SongFormations.concertSongId eq concertSongId }
        ConcertSongs.deleteWhere { ConcertSongs.id eq concertSongId } > 0
    }

    fun reorder(concertId: UUID, concertSongIds: List<UUID>) = transaction {
        concertSongIds.forEachIndexed { index, csId ->
            ConcertSongs.update({ ConcertSongs.id eq csId }) { it[sortOrder] = index }
        }
    }
}
