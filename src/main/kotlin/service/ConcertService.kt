package service

import model.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

data class ConcertDTO(val id: UUID, val name: String)

object ConcertService {

    fun list(): List<ConcertDTO> = transaction {
        Concerts.selectAll()
            .map { ConcertDTO(it[Concerts.id], it[Concerts.name]) }
    }

    fun create(name: String): ConcertDTO = transaction {
        val id = Concerts.insert {
            it[Concerts.name] = name
        } get Concerts.id

        ConcertDTO(id, name)
    }

    fun rename(id: UUID, newName: String): Boolean = transaction {
        Concerts.update({ Concerts.id eq id }) {
            it[name] = newName
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        val songIds = ConcertSongs.selectAll() // find every song belonging to this concert
            .where { ConcertSongs.concertId eq id }
            .map { it[ConcertSongs.id] }

        songIds.forEach { sId -> // delete rows that depend on songIds
            HiddenChorists.deleteWhere { HiddenChorists.concertSongId eq sId }
            SongFormations.deleteWhere { SongFormations.concertSongId eq sId }
        }
        ConcertSongs.deleteWhere { ConcertSongs.concertId eq id } // delete songs in the concert

        val formationIds = Formations.selectAll() // find every formation belonging to this concert
            .where { Formations.concertId eq id }
            .map { it[Formations.id] }

        formationIds.forEach { fId -> // delete all Placement rows whose formationId matches this formation
            Placements.deleteWhere { formationId eq fId }
        }
        Formations.deleteWhere { concertId eq id } // delete all formations whose concertId matches this concert
        ConcertChorists.deleteWhere { concertId eq id } // delete everyone in the roster for this concert
        Concerts.deleteWhere { Concerts.id eq id } > 0 // > 0 turns delete count into a boolean, returns true if a concert was deleted
    }

    fun duplicate(id: UUID, newName: String): ConcertDTO? = transaction {
        val original = Concerts.selectAll()
            .where { Concerts.id eq id }
            .firstOrNull() ?: return@transaction null

        val newConcertId = Concerts.insert {
            it[name] = newName
        } get Concerts.id

        val formations = Formations.selectAll()
            .where { Formations.concertId eq id }
            .toList()

        formations.forEach { f ->
            val oldFormationId = f[Formations.id]
            val newFormationId = Formations.insert {
                it[concertId] = newConcertId
                it[name] = f[Formations.name]
            } get Formations.id

            Placements.selectAll()
                .where { Placements.formationId eq oldFormationId }
                .forEach { p ->
                    Placements.insert {
                        it[formationId] = newFormationId
                        it[choristId] = p[Placements.choristId]
                        it[gridX] = p[Placements.gridX]
                        it[gridY] = p[Placements.gridY]
                    }
                }
        }

        // TODO: also duplicate Songs, SongFormations, HiddenChorists, and ConcertChorists

        ConcertDTO(newConcertId, newName)
    }
}
