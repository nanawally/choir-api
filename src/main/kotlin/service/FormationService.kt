package service

import model.ConcertChorists
import model.Formations
import model.Placements
import model.SongFormations
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

data class FormationDTO(val id: UUID, val name: String, val rowSizes: String)
data class PlacementDTO(val choristId: UUID, val gridX: Int, val gridY: Int)
data class FormationWithPlacements(
    val id: UUID,
    val name: String,
    val placements: List<PlacementDTO>,
    val rowSizes: String,
)

object FormationService {

    fun listByConcert(concertId: UUID): List<FormationDTO> = transaction {
        Formations.selectAll()
            .where { Formations.concertId eq concertId }
            .orderBy(Formations.name)
            .map { FormationDTO(it[Formations.id], it[Formations.name], it[Formations.rowSizes]) }
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
            formation[Formations.rowSizes],
        )
    }

    fun create(concertId: UUID, name: String): FormationDTO = transaction {
        val id = Formations.insert {
            it[Formations.concertId] = concertId
            it[Formations.name] = name
        } get Formations.id

        FormationDTO(id, name, "[]")
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
            it[rowSizes] = original[Formations.rowSizes]
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

        FormationDTO(newId, original[Formations.name] + " (copy)", original[Formations.rowSizes])
    }

    fun copyToConcert(formationId: UUID, targetConcertId: UUID): FormationDTO? = transaction {
        val original = Formations.selectAll()
            .where { Formations.id eq formationId }
            .firstOrNull() ?: return@transaction null

        val newId = Formations.insert {
            it[concertId] = targetConcertId
            it[name] = original[Formations.name]
            it[rowSizes] = original[Formations.rowSizes]
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

        FormationDTO(newId, original[Formations.name], original[Formations.rowSizes])
    }

    fun rename(formationId: UUID, newName: String): Boolean = transaction {
        Formations.update({ Formations.id eq formationId }) {
            it[name] = newName
        } > 0
    }

    fun updateRowSizes(formationId: UUID, rowSizes: String): Boolean = transaction {
        Formations.update ({ Formations.id eq formationId }) {
            it[Formations.rowSizes] = rowSizes
        } > 0
    }

    fun listBase(): List<FormationDTO> = transaction {
        Formations.selectAll()
            .where { Formations.concertId.isNull() }
            .orderBy(Formations.name)
            .map { FormationDTO(it[Formations.id], it[Formations.name], it[Formations.rowSizes]) }
    }

    fun createBase(name: String): FormationDTO = transaction {
        val id = Formations.insert {
            it[Formations.concertId] = null
            it[Formations.name] = name
        } get Formations.id

        FormationDTO(id, name, "[]")
    }

    fun copyBaseIntoConcert(formationId: UUID, concertId: UUID): FormationDTO? = transaction {
        val original = Formations.selectAll()
            .where { Formations.id eq formationId }
            .firstOrNull() ?: return@transaction null

        // Only allow copying base formations (null concertId)
        if (original[Formations.concertId] != null) return@transaction null

        val newId = Formations.insert {
            it[Formations.concertId] = concertId
            it[name] = original[Formations.name]
            it[rowSizes] = original[Formations.rowSizes]
        } get Formations.id

        // Get the concert roster to filter placements
        val concertChoristIds = ConcertChorists.selectAll()
            .where { ConcertChorists.concertId eq concertId }
            .map { it[ConcertChorists.choristId] }
            .toSet()

        Placements.selectAll()
            .where { Placements.formationId eq formationId }
            .forEach { p ->
                if (p[Placements.choristId] in concertChoristIds) {
                    Placements.insert {
                        it[Placements.formationId] = newId
                        it[choristId] = p[Placements.choristId]
                        it[gridX] = p[Placements.gridX]
                        it[gridY] = p[Placements.gridY]
                    }
                }
            }

        FormationDTO(newId, original[Formations.name], original[Formations.rowSizes])
    }
}
