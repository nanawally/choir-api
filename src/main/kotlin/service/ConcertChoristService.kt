package service

import model.ConcertChorists
import model.Formations
import model.Placements
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object ConcertChoristService {

    fun listByConcert(concertId: UUID): List<UUID> = transaction {
        ConcertChorists.selectAll()
            .where { ConcertChorists.concertId eq concertId }
            .map { it[ConcertChorists.choristId] }
    }

    fun setByConcert(concertId: UUID, choristIds: List<UUID>): Unit = transaction {
        val newIds = choristIds.toSet()
        val currentIds = ConcertChorists.selectAll()
            .where { ConcertChorists.concertId eq concertId }
            .map { it[ConcertChorists.choristId] }
            .toSet()
        val removedIds = currentIds - newIds

        ConcertChorists.deleteWhere { ConcertChorists.concertId eq concertId }
        choristIds.forEach { cId ->
            ConcertChorists.insert {
                it[ConcertChorists.concertId] = concertId
                it[ConcertChorists.choristId] = cId
            }
        }

        if (removedIds.isNotEmpty()) {
            val formationIds = Formations.selectAll()
                .where { Formations.concertId eq concertId }
                .map { it[Formations.id] }
            if (formationIds.isNotEmpty()) {
                Placements.deleteWhere {
                    (Placements.formationId inList formationIds) and
                    (Placements.choristId inList removedIds)
                }
            }
        }
    }
}
