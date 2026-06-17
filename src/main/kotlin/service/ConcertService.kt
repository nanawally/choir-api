package service

import model.Concerts
import model.Formations
import model.HiddenChorists
import model.Placements
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
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
        val formationIds = Formations.selectAll()
            .where { Formations.concertId eq id }
            .map { it[Formations.id] }

        formationIds.forEach { fId ->
            HiddenChorists.deleteWhere { formationId eq fId }
            Placements.deleteWhere { formationId eq fId }
        }
        Formations.deleteWhere { concertId eq id }
        Concerts.deleteWhere { Concerts.id eq id } > 0
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
            .orderBy(Formations.sortOrder)
            .toList()

        formations.forEach { f ->
            val oldFormationId = f[Formations.id]
            val newFormationId = Formations.insert {
                it[concertId] = newConcertId
                it[name] = f[Formations.name]
                it[sortOrder] = f[Formations.sortOrder]
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

            HiddenChorists.selectAll()
                .where { HiddenChorists.formationId eq oldFormationId }
                .forEach { h ->
                    HiddenChorists.insert {
                        it[formationId] = newFormationId
                        it[choristId] = h[HiddenChorists.choristId]
                    }
                }
        }

        ConcertDTO(newConcertId, newName)
    }
}
