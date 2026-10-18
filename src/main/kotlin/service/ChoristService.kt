package service

import model.Chorists
import model.HiddenChorists
import model.Placements
import model.VoiceAssignments
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class ChoristDTO(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val isSectionLeader: Boolean,
    val isArchived: Boolean,
    val notes: String?,
)

object ChoristService {

    fun list(choirId: UUID, includeArchived: Boolean = false): List<ChoristDTO> = transaction {
        Chorists.selectAll()
            .where { Chorists.choirId eq choirId }
            .let { if (!includeArchived) it.andWhere { Chorists.isArchived eq false } else it }
            .map {
                ChoristDTO(
                    it[Chorists.id],
                    it[Chorists.firstName],
                    it[Chorists.lastName],
                    it[Chorists.isSectionLeader],
                    it[Chorists.isArchived],
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

        ChoristDTO(id, firstName, lastName, isSectionLeader, false, null)
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

    fun delete(id: UUID, choirId: UUID): Boolean = transaction {
        // Verify ownership before cascading deletes
        val exists = Chorists.selectAll()
            .where { (Chorists.id eq id) and (Chorists.choirId eq choirId) }
            .count() > 0
        if (!exists) return@transaction false

        HiddenChorists.deleteWhere { choristId eq id }
        Placements.deleteWhere { choristId eq id }
        VoiceAssignments.deleteWhere { choristId eq id }
        Chorists.deleteWhere { (Chorists.id eq id) and (Chorists.choirId eq choirId) } > 0
    }
}
