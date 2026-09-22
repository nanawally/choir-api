package service

import model.VoiceAssignments
import model.VoiceGroups
import model.VoiceParts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class VoicePartDTO(val id: UUID, val name: String, val color: String, val
shape: String)
data class VoiceGroupDTO(val id: UUID, val name: String, val isStandard: Boolean, val parts:
List<VoicePartDTO>)
data class VoiceAssignmentDTO(val choristId: UUID, val voicePartId: UUID)

object VoiceGroupService {

    fun list(): List<VoiceGroupDTO> = transaction {
        val groups = VoiceGroups.selectAll().map { row ->
            val groupId = row[VoiceGroups.id]
            val parts = VoiceParts.selectAll()
                .where { VoiceParts.voiceGroupId eq groupId }
                .orderBy(VoiceParts.sortOrder)
                .map { VoicePartDTO(it[VoiceParts.id], it[VoiceParts.name],
                    it[VoiceParts.color], it[VoiceParts.shape]) }
            VoiceGroupDTO(groupId, row[VoiceGroups.name], row[VoiceGroups.isStandard], parts)
        }
        groups
    }

    fun create(name: String, isStandard: Boolean = false): VoiceGroupDTO = transaction {
        val id = VoiceGroups.insert {
            it[VoiceGroups.name] = name
            it[VoiceGroups.isStandard] = isStandard
        } get VoiceGroups.id
        VoiceGroupDTO(id, name, isStandard, emptyList())
    }

    fun rename(id: UUID, name: String): Boolean = transaction {
        VoiceGroups.update({ VoiceGroups.id eq id }) {
            it[VoiceGroups.name] = name
        } > 0
    }

    fun setStandard(id: UUID, isStandard: Boolean): Boolean = transaction {
        VoiceGroups.update({ VoiceGroups.id eq id }) {
            it[VoiceGroups.isStandard] = isStandard
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        val partIds = VoiceParts.selectAll()
            .where { VoiceParts.voiceGroupId eq id }
            .map { it[VoiceParts.id] }
        VoiceAssignments.deleteWhere { voicePartId inList partIds }
        VoiceParts.deleteWhere { voiceGroupId eq id }
        VoiceGroups.deleteWhere { VoiceGroups.id eq id } > 0
    }

    fun addPart(groupId: UUID, name: String, color: String, shape: String):
            VoicePartDTO = transaction {
        val maxOrder = VoiceParts.selectAll()
            .where { VoiceParts.voiceGroupId eq groupId }
            .maxOfOrNull { it[VoiceParts.sortOrder] } ?: 0
        val id = VoiceParts.insert {
            it[voiceGroupId] = groupId
            it[VoiceParts.name] = name
            it[VoiceParts.color] = color
            it[VoiceParts.shape] = shape
            it[sortOrder] = maxOrder + 1
        } get VoiceParts.id
        VoicePartDTO(id, name, color, shape)
    }

    fun updatePart(partId: UUID, newName: String, newColor: String, newShape: String): Boolean = transaction {
        VoiceParts.update({ VoiceParts.id eq partId }) {
            it[name] = newName
            it[color] = newColor
            it[shape] = newShape
        } > 0
    }

    fun reorderParts(partIds: List<UUID>) = transaction {
        partIds.forEachIndexed { index, partId ->
            VoiceParts.update({ VoiceParts.id eq partId }) { it[sortOrder] = index }
        }
    }

    fun deletePart(partId: UUID): Boolean = transaction {
        VoiceAssignments.deleteWhere { voicePartId eq partId }
        VoiceParts.deleteWhere { VoiceParts.id eq partId } > 0
    }

    fun getAssignments(groupId: UUID): List<VoiceAssignmentDTO> = transaction {
        val partIds = VoiceParts.selectAll()
            .where { VoiceParts.voiceGroupId eq groupId }
            .map { it[VoiceParts.id] }

        VoiceAssignments.selectAll()
            .where { VoiceAssignments.voicePartId inList partIds }
            .map { VoiceAssignmentDTO(it[VoiceAssignments.choristId], it[VoiceAssignments.voicePartId]) }
    }

    fun assign(choristId: UUID, voicePartId: UUID): Unit = transaction {
        // Find which voice group this part belongs to
        val groupId = VoiceParts.selectAll()
            .where { VoiceParts.id eq voicePartId }
            .single()[VoiceParts.voiceGroupId]

        // Get all part IDs in this group
        val partIds = VoiceParts.selectAll()
            .where { VoiceParts.voiceGroupId eq groupId }
            .map { it[VoiceParts.id] }

        // Remove any existing assignment for this chorist in this group
        VoiceAssignments.deleteWhere {
            (VoiceAssignments.choristId eq choristId) and (VoiceAssignments.voicePartId inList partIds)
        }

        // Insert new assignment
        VoiceAssignments.insert {
            it[VoiceAssignments.choristId] = choristId
            it[VoiceAssignments.voicePartId] = voicePartId
        }
    }

    fun unassign(choristId: UUID, groupId: UUID): Unit = transaction {
        val partIds = VoiceParts.selectAll()
            .where { VoiceParts.voiceGroupId eq groupId }
            .map { it[VoiceParts.id] }

        VoiceAssignments.deleteWhere {
            (VoiceAssignments.choristId eq choristId) and (VoiceAssignments.voicePartId inList partIds)
        }
    }
}
