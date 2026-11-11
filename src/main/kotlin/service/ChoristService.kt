package service

import model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class ChoristDTO(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val isSectionLeader: Boolean,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val notes: String?,
)

object ChoristService {

    fun getById(id: UUID, choirId: UUID): ChoristDTO? = transaction {
        Chorists.selectAll()
            .where { (Chorists.id eq id) and (Chorists.choirId eq choirId) }
            .firstOrNull()?.let {
                ChoristDTO(it[Chorists.id], it[Chorists.firstName], it[Chorists.lastName],
                    it[Chorists.isSectionLeader], it[Chorists.isArchived], it[Chorists.isDeleted], it[Chorists.notes])
            }
    }

    fun list(choirId: UUID, includeArchived: Boolean = false): List<ChoristDTO> = transaction {
        Chorists.selectAll()
            .where { (Chorists.choirId eq choirId) and (Chorists.isDeleted eq false) }
            .let { if (!includeArchived) it.andWhere { Chorists.isArchived eq false } else it }
            .map {
                ChoristDTO(
                    it[Chorists.id],
                    it[Chorists.firstName],
                    it[Chorists.lastName],
                    it[Chorists.isSectionLeader],
                    it[Chorists.isArchived],
                    it[Chorists.isDeleted],
                    it[Chorists.notes],
                )
            }
    }

    fun create(choirId: UUID, firstName: String, lastName: String, isSectionLeader: Boolean = false): ChoristDTO = transaction {
        val id = Chorists.insert {
            it[Chorists.choirId] = choirId
            it[Chorists.firstName] = firstName
            it[Chorists.lastName] = lastName
            it[Chorists.isSectionLeader] = isSectionLeader
        } get Chorists.id

        ChoristDTO(id, firstName, lastName, isSectionLeader, isArchived = false, isDeleted = false, notes = null)
    }

    fun update(id: UUID, choirId: UUID, firstName: String, lastName: String, isSectionLeader: Boolean, notes: String?): Boolean = transaction {
        Chorists.update({ (Chorists.id eq id) and (Chorists.choirId eq choirId) }) {
            it[Chorists.firstName] = firstName
            it[Chorists.lastName] = lastName
            it[Chorists.isSectionLeader] = isSectionLeader
            it[Chorists.notes] = notes
        } > 0
    }

    fun archive(id: UUID, choirId: UUID): Boolean = transaction {
        Chorists.update({ (Chorists.id eq id) and (Chorists.choirId eq choirId) }) {
            it[isArchived] = true
        } > 0
    }

    fun unarchive(id: UUID, choirId: UUID): Boolean = transaction {
        Chorists.update({ (Chorists.id eq id) and (Chorists.choirId eq choirId) }) {
            it[isArchived] = false
        } > 0
    }

    fun delete(id: UUID, choirId: UUID, deleteUser: Boolean = true): Boolean = transaction {
        // Verify ownership before cascading deletes
        val chorist = Chorists.selectAll()
            .where { (Chorists.id eq id) and (Chorists.choirId eq choirId) }
            .firstOrNull() ?: return@transaction false

        // Cascade-delete associated data (but keep concert formation placements)
        HiddenChorists.deleteWhere { choristId eq id }
        ConcertChorists.deleteWhere { ConcertChorists.choristId eq id }
        VoiceAssignments.deleteWhere { choristId eq id }
        if (deleteUser) {
            Users.deleteWhere { Users.choristId eq id }
        } else {
            // Unlink the user account from the chorist
            Users.update({ Users.choristId eq id }) {
                it[Users.choristId] = null
            }
        }

        // Remove placements from base formations only (concertId IS NULL)
        val baseFormationIds = Formations.selectAll()
            .where { Formations.concertId.isNull() }
            .map { it[Formations.id] }
        if (baseFormationIds.isNotEmpty()) {
            Placements.deleteWhere {
                (Placements.choristId eq id) and (Placements.formationId inList baseFormationIds)
            }
        }

        // Anonymize the chorist record: keep firstName, truncate lastName, clear notes
        val lastName = chorist[Chorists.lastName]
        val anonymizedLastName = if (lastName.isNotEmpty()) "${lastName.first()}." else ""
        Chorists.update({ (Chorists.id eq id) and (Chorists.choirId eq choirId) }) {
            it[Chorists.lastName] = anonymizedLastName
            it[Chorists.notes] = null
            it[Chorists.isSectionLeader] = false
            it[Chorists.isDeleted] = true
        } > 0
    }
}
