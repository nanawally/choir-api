package service

import model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class FormationDTO(val id: UUID, val name: String, val rowSizes: String)
data class PlacementDTO(val choristId: UUID, val gridX: Int, val gridY: Int)
data class GhostChoristDTO(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val isArchived: Boolean,
    val isDeleted: Boolean,
)

data class FormationWithPlacements(
    val id: UUID,
    val name: String,
    val placements: List<PlacementDTO>,
    val rowSizes: String,
    val ghostChorists: List<GhostChoristDTO> = emptyList(),
)

object FormationService {

    private fun copyPlacements(fromId: UUID, toId: UUID, choristFilter: Set<UUID>? = null) {
        Placements.selectAll()
            .where { Placements.formationId eq fromId }
            .let { query -> if (choristFilter != null) query.filter { it[Placements.choristId] in choristFilter } else query.toList() }
            .forEach { p ->
                Placements.insert {
                    it[formationId] = toId
                    it[choristId] = p[Placements.choristId]
                    it[gridX] = p[Placements.gridX]
                    it[gridY] = p[Placements.gridY]
                }
            }
    }

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

        // Find ghost chorists: placed chorists who are archived or deleted
        val placedChoristIds = placements.map { it.choristId }
        val ghostChorists = if (placedChoristIds.isNotEmpty()) {
            Chorists.selectAll()
                .where { (Chorists.id inList placedChoristIds) and ((Chorists.isArchived eq true) or (Chorists.isDeleted eq true)) }
                .map { GhostChoristDTO(it[Chorists.id], it[Chorists.firstName], it[Chorists.lastName], it[Chorists.isArchived], it[Chorists.isDeleted]) }
        } else emptyList()

        FormationWithPlacements(
            formation[Formations.id],
            formation[Formations.name],
            placements,
            formation[Formations.rowSizes],
            ghostChorists,
        )
    }

    fun listByConcertWithPlacements(concertId: UUID): List<FormationWithPlacements> = transaction {
        val formations = Formations.selectAll()
            .where { Formations.concertId eq concertId }
            .orderBy(Formations.name)
            .toList()

        if (formations.isEmpty()) return@transaction emptyList()

        val formationIds = formations.map { it[Formations.id] }

        val placementsByFormation = Placements.selectAll()
            .where { Placements.formationId inList formationIds }
            .groupBy { it[Placements.formationId] }
            .mapValues { (_, rows) ->
                rows.map { PlacementDTO(it[Placements.choristId], it[Placements.gridX], it[Placements.gridY]) }
            }

        val allPlacedChoristIds = placementsByFormation.values.flatten().map { it.choristId }.toSet()
        val ghostChoristMap = if (allPlacedChoristIds.isNotEmpty()) {
            Chorists.selectAll()
                .where { (Chorists.id inList allPlacedChoristIds) and ((Chorists.isArchived eq true) or (Chorists.isDeleted eq true)) }
                .map { GhostChoristDTO(it[Chorists.id], it[Chorists.firstName], it[Chorists.lastName], it[Chorists.isArchived], it[Chorists.isDeleted]) }
                .associateBy { it.id }
        } else emptyMap()

        formations.map { row ->
            val fId = row[Formations.id]
            val placements = placementsByFormation[fId] ?: emptyList()
            val ghosts = placements.mapNotNull { ghostChoristMap[it.choristId] }.distinctBy { it.id }
            FormationWithPlacements(fId, row[Formations.name], placements, row[Formations.rowSizes], ghosts)
        }
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

        copyPlacements(id, newId)

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

        copyPlacements(formationId, newId)

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

    fun listBase(choirId: UUID): List<FormationDTO> = transaction {
        Formations.selectAll()
            .where { Formations.concertId.isNull() and (Formations.choirId eq choirId) }
            .orderBy(Formations.name)
            .map { FormationDTO(it[Formations.id], it[Formations.name], it[Formations.rowSizes]) }
    }

    fun createBase(choirId: UUID, name: String): FormationDTO = transaction {
        val id = Formations.insert {
            it[Formations.choirId] = choirId
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

        // Only copy placements for chorists on the concert roster
        val concertChoristIds = ConcertChorists.selectAll()
            .where { ConcertChorists.concertId eq concertId }
            .map { it[ConcertChorists.choristId] }
            .toSet()

        copyPlacements(formationId, newId, concertChoristIds)

        FormationDTO(newId, original[Formations.name], original[Formations.rowSizes])
    }
}
