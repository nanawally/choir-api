package service

import model.Formations
import model.HiddenChorists
import model.Placements
import model.SongFormations
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class FormationDTO(val id: UUID, val name: String)
data class PlacementDTO(val choristId: UUID, val gridX: Int, val gridY: Int)
data class FormationWithPlacements(
    val id: UUID,
    val name: String,
    val placements: List<PlacementDTO>,
)

object FormationService {

    fun listByConcert(concertId: UUID): List<FormationDTO> = transaction {
        Formations.selectAll()
            .where { Formations.concertId eq concertId }
            .orderBy(Formations.name)
            .map { FormationDTO(it[Formations.id], it[Formations.name]) }
    }

    fun get(id: UUID): FormationWithPlacements? = transaction {
        val formation = Formations.selectAll()
            .where { Formations.id eq id }
            .firstOrNull() ?: return@transaction null

        val placements = Placements.selectAll()
            .where { Placements.formationId eq id }
            .map { PlacementDTO(it[Placements.choristId], it[Placements.gridX], it[Placements.gridY]) }

        FormationWithPlacements(
            formation[Formations.id],
            formation[Formations.name],
            placements,
        )
    }

    fun create(concertId: UUID, name: String): FormationDTO = transaction {
        val id = Formations.insert {
            it[Formations.concertId] = concertId
            it[Formations.name] = name
        } get Formations.id

        FormationDTO(id, name)
    }

    fun delete(id: UUID): Boolean = transaction {
        SongFormations.deleteWhere { formationId eq id }
        Placements.deleteWhere { formationId eq id }
        Formations.deleteWhere { Formations.id eq id } > 0
    }

    fun savePlacements(formationId: UUID, placements: List<PlacementDTO>): Unit = transaction {
        Placements.deleteWhere { Placements.formationId eq formationId }
        placements.forEach { p ->
            Placements.insert {
                it[Placements.formationId] = formationId
                it[choristId] = p.choristId
                it[gridX] = p.gridX
                it[gridY] = p.gridY
            }
        }
    }

    fun duplicate(id: UUID): FormationDTO? = transaction {
        val original = Formations.selectAll()
            .where { Formations.id eq id }
            .firstOrNull() ?: return@transaction null

        val concertId = original[Formations.concertId]

        val newId = Formations.insert {
            it[Formations.concertId] = concertId
            it[name] = original[Formations.name] + " (copy)"
        } get Formations.id

        Placements.selectAll()
            .where { Placements.formationId eq id }
            .forEach { p ->
                Placements.insert {
                    it[formationId] = newId
                    it[choristId] = p[Placements.choristId]
                    it[gridX] = p[Placements.gridX]
                    it[gridY] = p[Placements.gridY]
                }
            }

        FormationDTO(newId, original[Formations.name] + " (copy)")
    }

    fun copyToConcert(formationId: UUID, targetConcertId: UUID): FormationDTO? = transaction {
        val original = Formations.selectAll()
            .where { Formations.id eq formationId }
            .firstOrNull() ?: return@transaction null

        val newId = Formations.insert {
            it[concertId] = targetConcertId
            it[name] = original[Formations.name]
        } get Formations.id

        Placements.selectAll()
            .where { Placements.formationId eq formationId }
            .forEach { p ->
                Placements.insert {
                    it[Placements.formationId] = newId
                    it[choristId] = p[Placements.choristId]
                    it[gridX] = p[Placements.gridX]
                    it[gridY] = p[Placements.gridY]
                }
            }

        FormationDTO(newId, original[Formations.name])
    }
}
