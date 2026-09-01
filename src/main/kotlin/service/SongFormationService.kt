package service

import model.SongFormations
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object SongFormationService {

    fun listBySong(songId: UUID): List<UUID> = transaction {
        SongFormations.selectAll()
            .where { SongFormations.songId eq songId }
            .orderBy(SongFormations.sortOrder)
            .map { it[SongFormations.formationId] }
    }

    fun setBySong(songId: UUID, songFormationId: List<UUID>) = transaction {
        SongFormations.deleteWhere { SongFormations.songId eq songId }
        songFormationId.forEachIndexed { index, fId ->
            SongFormations.insert {
                it[SongFormations.songId] = songId
                it[SongFormations.formationId] = fId
                it[SongFormations.sortOrder] = index
            }
        }
    }
}
