package service

import model.ConcertChorists
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
        ConcertChorists.deleteWhere { ConcertChorists.concertId eq concertId }
        choristIds.forEach { cId ->
            ConcertChorists.insert {
                it[ConcertChorists.concertId] = concertId
                it[ConcertChorists.choristId] = cId
            }
        }
    }
}
