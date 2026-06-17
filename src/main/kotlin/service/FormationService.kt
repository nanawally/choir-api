package service

import model.Formations
import model.HiddenChorists
import model.Placements
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class FormationDTO(val id: UUID, val name: String, val sortOrder: Int)
data class PlacementDTO(val choristId: UUID, val gridX: Int, val gridY: Int)
data class FormationWithPlacements(
    val id: UUID,
    val name: String,
    val sortOrder: Int,
    val placements: List<PlacementDTO>,
    val hiddenChoristIds: List<UUID>,
)

object FormationService {

    fun listByConcert(concertId: UUID): List<FormationDTO> = transaction {
        Formations.selectAll()
            .where { Formations.concertId eq concertId }
            .orderBy(Formations.sortOrder)
            .map { FormationDTO(it[Formations.id], it[Formations.name], it[Formations.sortOrder]) }
    }

    fun get(id: UUID): FormationWithPlacements? = transaction {
        val formation = Formations.selectAll()
            .where { Formations.id eq id }
            .firstOrNull() ?: return@transaction null

        val placements = Placements.selectAll()
            .where { Placements.formationId eq id }
            .map { PlacementDTO(it[Placements.choristId], it[Placements.gridX], it[Placements.gridY]) }

        val hiddenIds = HiddenChorists.selectAll()
            .where { HiddenChorists.formationId eq id }
            .map { it[HiddenChorists.choristId] }

        FormationWithPlacements(
            formation[Formations.id],
            formation[Formations.name],
            formation[Formations.sortOrder],
            placements,
            hiddenIds,
        )
    }

    fun create(concertId: UUID, name: String): FormationDTO = transaction {
        val maxOrder = Formations.selectAll()
            .where { Formations.concertId eq concertId }
            .maxOfOrNull { it[Formations.sortOrder] } ?: -1

        val id = Formations.insert {
            it[Formations.concertId] = concertId
            it[Formations.name] = name
            it[sortOrder] = maxOrder + 1
        } get Formations.id

        FormationDTO(id, name, maxOrder + 1)
    }

    fun delete(id: UUID): Boolean = transaction {
        HiddenChorists.deleteWhere { formationId eq id }
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

    fun setHiddenChorists(formationId: UUID, choristIds: List<UUID>): Unit = transaction {
        HiddenChorists.deleteWhere { HiddenChorists.formationId eq formationId }
        choristIds.forEach { cId ->
            HiddenChorists.insert {
                it[HiddenChorists.formationId] = formationId
                it[choristId] = cId
            }
        }
    }

    fun duplicate(id: UUID): FormationDTO? = transaction {
        val original = Formations.selectAll()
            .where { Formations.id eq id }
            .firstOrNull() ?: return@transaction null

        val concertId = original[Formations.concertId]
        val maxOrder = Formations.selectAll()
            .where { Formations.concertId eq concertId }
            .maxOfOrNull { it[Formations.sortOrder] } ?: -1

        val newId = Formations.insert {
            it[Formations.concertId] = concertId
            it[name] = original[Formations.name] + " (copy)"
            it[sortOrder] = maxOrder + 1
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

        HiddenChorists.selectAll()
            .where { HiddenChorists.formationId eq id }
            .forEach { h ->
                HiddenChorists.insert {
                    it[formationId] = newId
                    it[choristId] = h[HiddenChorists.choristId]
                }
            }

        FormationDTO(newId, original[Formations.name] + " (copy)", maxOrder + 1)
    }

    fun copyToConcert(formationId: UUID, targetConcertId: UUID): FormationDTO? = transaction {
        val original = Formations.selectAll()
            .where { Formations.id eq formationId }
            .firstOrNull() ?: return@transaction null

        val maxOrder = Formations.selectAll()
            .where { Formations.concertId eq targetConcertId }
            .maxOfOrNull { it[Formations.sortOrder] } ?: -1

        val newId = Formations.insert {
            it[concertId] = targetConcertId
            it[name] = original[Formations.name]
            it[sortOrder] = maxOrder + 1
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

        HiddenChorists.selectAll()
            .where { HiddenChorists.formationId eq formationId }
            .forEach { h ->
                HiddenChorists.insert {
                    it[HiddenChorists.formationId] = newId
                    it[HiddenChorists.choristId] = h[HiddenChorists.choristId]
                }
            }

        FormationDTO(newId, original[Formations.name], maxOrder + 1)
    }

    fun reorder(concertId: UUID, formationIds: List<UUID>): Unit = transaction {
        formationIds.forEachIndexed { index, fId ->
            Formations.update({ Formations.id eq fId }) {
                it[sortOrder] = index
            }
        }
    }
}
