package model

import org.jetbrains.exposed.sql.Table

object PermissionGroups : Table("permission_groups") {
    val id      = uuid("id").autoGenerate()
    val choirId = uuid("choir_id").references(Choirs.id)
    val name    = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object GroupPermissions : Table("group_permissions") {
    val id        = uuid("id").autoGenerate()
    val groupId   = uuid("group_id").references(PermissionGroups.id)
    val feature   = varchar("feature", 100)
    val canView   = bool("can_view").default(false)
    val canEdit   = bool("can_edit").default(false)
    val canDelete = bool("can_delete").default(false)
    override val primaryKey = PrimaryKey(id)
}

object UserGroups : Table("user_groups") {
    val id      = uuid("id").autoGenerate()
    val userId  = uuid("user_id").references(Users.id)
    val groupId = uuid("group_id").references(PermissionGroups.id)
    override val primaryKey = PrimaryKey(id)
}
