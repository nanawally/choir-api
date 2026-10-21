package service

import model.GroupPermissions
import model.PermissionGroups
import model.UserGroups
import model.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

val ALL_FEATURES = listOf(
    "chorists", "chorists_notes",
    "voice_groups",
    "songs",
    "songbooks", "songbooks_hidden",
    "concerts",
    "formations",
    "base_formations",
    "permissions",
)

data class PermissionDTO(
    val feature: String,
    val canView: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
)

data class PermissionGroupDTO(
    val id: UUID,
    val name: String,
    val permissions: List<PermissionDTO>,
    val memberIds: List<UUID>,
)

object PermissionService {

    /** True if the user has the given action on a feature (isSuperUser bypasses all checks). */
    fun check(userId: UUID, feature: String, action: String): Boolean = transaction {
        val user = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: return@transaction false
        if (user[Users.isSuperUser]) return@transaction true

        val groupIds = UserGroups.selectAll()
            .where { UserGroups.userId eq userId }
            .map { it[UserGroups.groupId] }
        if (groupIds.isEmpty()) return@transaction false

        val col = when (action) {
            "view"   -> GroupPermissions.canView
            "edit"   -> GroupPermissions.canEdit
            "delete" -> GroupPermissions.canDelete
            else     -> return@transaction false
        }
        GroupPermissions.selectAll().where {
            (GroupPermissions.groupId inList groupIds) and
            (GroupPermissions.feature eq feature) and
            (col eq true)
        }.count() > 0
    }

    /** Returns the effective permission map for a user (used by GET /me/permissions). */
    fun getEffective(userId: UUID): Map<String, PermissionDTO> = transaction {
        val user = Users.selectAll().where { Users.id eq userId }.singleOrNull()
            ?: return@transaction emptyMap()

        if (user[Users.isSuperUser]) {
            return@transaction ALL_FEATURES.associateWith { f ->
                PermissionDTO(f, canView = true, canEdit = true, canDelete = true)
            }
        }

        val groupIds = UserGroups.selectAll()
            .where { UserGroups.userId eq userId }
            .map { it[UserGroups.groupId] }

        val rows = if (groupIds.isEmpty()) emptyList()
        else GroupPermissions.selectAll()
            .where { GroupPermissions.groupId inList groupIds }
            .toList()

        ALL_FEATURES.associateWith { feature ->
            val matching = rows.filter { it[GroupPermissions.feature] == feature }
            PermissionDTO(
                feature   = feature,
                canView   = matching.any { it[GroupPermissions.canView] },
                canEdit   = matching.any { it[GroupPermissions.canEdit] },
                canDelete = matching.any { it[GroupPermissions.canDelete] },
            )
        }
    }

    fun listGroups(choirId: UUID): List<PermissionGroupDTO> = transaction {
        val groups = PermissionGroups.selectAll()
            .where { PermissionGroups.choirId eq choirId }
            .map { it[PermissionGroups.id] to it[PermissionGroups.name] }

        groups.map { (gId, gName) ->
            val perms = GroupPermissions.selectAll()
                .where { GroupPermissions.groupId eq gId }
                .map { row ->
                    PermissionDTO(
                        feature   = row[GroupPermissions.feature],
                        canView   = row[GroupPermissions.canView],
                        canEdit   = row[GroupPermissions.canEdit],
                        canDelete = row[GroupPermissions.canDelete],
                    )
                }
            val members = UserGroups.selectAll()
                .where { UserGroups.groupId eq gId }
                .map { it[UserGroups.userId] }
            PermissionGroupDTO(gId, gName, perms, members)
        }
    }

    fun createGroup(choirId: UUID, name: String): PermissionGroupDTO = transaction {
        val id = PermissionGroups.insert {
            it[PermissionGroups.choirId] = choirId
            it[PermissionGroups.name]    = name
        } get PermissionGroups.id
        PermissionGroupDTO(id, name, emptyList(), emptyList())
    }

    fun deleteGroup(id: UUID, choirId: UUID): Boolean = transaction {
        // Prevent deleting the Admin group
        val group = PermissionGroups.selectAll()
            .where { (PermissionGroups.id eq id) and (PermissionGroups.choirId eq choirId) }
            .singleOrNull() ?: return@transaction false
        if (group[PermissionGroups.name] == "Admin") return@transaction false
        PermissionGroups.deleteWhere { PermissionGroups.id eq id } > 0
    }

    fun setPermission(groupId: UUID, feature: String, canView: Boolean, canEdit: Boolean, canDelete: Boolean): Boolean = transaction {
        val exists = GroupPermissions.selectAll()
            .where { (GroupPermissions.groupId eq groupId) and (GroupPermissions.feature eq feature) }
            .count() > 0
        if (exists) {
            GroupPermissions.update({ (GroupPermissions.groupId eq groupId) and (GroupPermissions.feature eq feature) }) {
                it[GroupPermissions.canView]   = canView
                it[GroupPermissions.canEdit]   = canEdit
                it[GroupPermissions.canDelete] = canDelete
            }
        } else {
            GroupPermissions.insert {
                it[GroupPermissions.groupId]   = groupId
                it[GroupPermissions.feature]   = feature
                it[GroupPermissions.canView]   = canView
                it[GroupPermissions.canEdit]   = canEdit
                it[GroupPermissions.canDelete] = canDelete
            }
        }
        true
    }

    fun addMember(groupId: UUID, userId: UUID): Boolean = transaction {
        val already = UserGroups.selectAll()
            .where { (UserGroups.groupId eq groupId) and (UserGroups.userId eq userId) }
            .count() > 0
        if (already) return@transaction false
        UserGroups.insert {
            it[UserGroups.groupId] = groupId
            it[UserGroups.userId]  = userId
        }
        true
    }

    fun removeMember(groupId: UUID, userId: UUID): Boolean = transaction {
        UserGroups.deleteWhere {
            (UserGroups.groupId eq groupId) and (UserGroups.userId eq userId)
        } > 0
    }
}
