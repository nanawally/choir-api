package service

import model.Chorists
import model.HiddenChorists
import model.Placements
import model.VoiceAssignments
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class ChoristDTO(
    val id: UUID,
    val name: String,
    val isSectionLeader: Boolean,
    val isArchived: Boolean,
)

object ChoristService {

    fun list(includeArchived: Boolean = false): List<ChoristDTO> = transaction {
        Chorists.selectAll()
            .let { if (!includeArchived) it.where { Chorists.isArchived eq false } else it }
            .map {
                ChoristDTO(
                    it[Chorists.id],
                    it[Chorists.name],
                    it[Chorists.isSectionLeader],
                    it[Chorists.isArchived],
                )
            }
    }

    fun create(name: String, isSectionLeader: Boolean = false): ChoristDTO = transaction {
        val id = Chorists.insert {
            it[Chorists.name] = name
            it[Chorists.isSectionLeader] = isSectionLeader
        } get Chorists.id

        ChoristDTO(id, name, isSectionLeader, false)
    }

    fun update(id: UUID, name: String, isSectionLeader: Boolean): Boolean = transaction {
        Chorists.update({ Chorists.id eq id }) {
            it[Chorists.name] = name
            it[Chorists.isSectionLeader] = isSectionLeader
        } > 0
    }

    fun archive(id: UUID): Boolean = transaction {
        Chorists.update({ Chorists.id eq id }) {
            it[isArchived] = true
        } > 0
    }

    fun unarchive(id: UUID): Boolean = transaction {
        Chorists.update({ Chorists.id eq id }) {
            it[isArchived] = false
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        HiddenChorists.deleteWhere { choristId eq id }
        Placements.deleteWhere { choristId eq id }
        VoiceAssignments.deleteWhere { choristId eq id }
        Chorists.deleteWhere { Chorists.id eq id } > 0
    }
}
