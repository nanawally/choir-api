package service

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

data class SongDTO(val id: UUID, val name: String, val sortOrder: Int, val formationId: UUID?)

object SongService {

    fun listByConcert(concertId: UUID): List<SongDTO> = transaction {

        Songs.selectAll()
            .where { Songs.concertId eq concertId }
            .orderBy(Songs.sortOrder)
            .map {
                val formationId = SongFormations.selectAll()
                    .where { SongFormations.songId eq it[Songs.id] }
                    .firstOrNull()?.get(SongFormations.formationId)
                SongDTO(it[Songs.id], it[Songs.name], it[Songs.sortOrder], formationId)
            }
    }

    fun create(concertId: UUID, name: String): SongDTO = transaction {
        val max = Songs.selectAll()
            .where { Songs.concertId eq concertId }
            .maxOfOrNull { it[Songs.sortOrder] } ?: 0

        val id = Songs.insert {
            it[Songs.concertId] = concertId
            it[Songs.name] = name
            it[Songs.sortOrder] = max + 1
        } get Songs.id

        SongDTO(id, name, max + 1, null)
    }

    fun rename(id: UUID, newName: String): Boolean = transaction {
        Songs.update({ Songs.id eq id }) {
            it[Songs.name] = newName
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        HiddenChorists.deleteWhere { HiddenChorists.songId eq id }
        SongFormations.deleteWhere { SongFormations.songId eq id }
        Songs.deleteWhere { Songs.id eq id } > 0
    }
}
