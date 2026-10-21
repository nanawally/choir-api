package routes

import auth.extractChoirId
import auth.extractUserId
import auth.requirePermission
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.PermissionGroupDTO
import service.PermissionService
import java.util.*

@Serializable
data class CreateGroupRequest(val name: String)

@Serializable
data class SetPermissionRequest(val feature: String, val canView: Boolean, val canEdit: Boolean, val canDelete: Boolean)

@Serializable
data class AddMemberRequest(val userId: String)

@Serializable
data class PermissionResponse(val feature: String, val canView: Boolean, val canEdit: Boolean, val canDelete: Boolean)

@Serializable
data class PermissionGroupResponse(val id: String, val name: String, val isAdmin: Boolean, val permissions: List<PermissionResponse>, val memberIds: List<String>)

@Serializable
data class MyPermissionsResponse(val isSuperUser: Boolean, val permissions: Map<String, PermissionResponse>)

private fun PermissionGroupDTO.toResponse() = PermissionGroupResponse(
    id          = id.toString(),
    name        = name,
    isAdmin     = name == "Admin",
    permissions = permissions.map { PermissionResponse(it.feature, it.canView, it.canEdit, it.canDelete) },
    memberIds   = memberIds.map { it.toString() },
)

fun Route.permissionRoutes() {
    route("/me/permissions") {
        get {
            val userId = extractUserId()
            val effective = PermissionService.getEffective(userId)
            val isSuperUser = service.UserService.isSuperUser(userId)
            call.respond(MyPermissionsResponse(
                isSuperUser = isSuperUser,
                permissions = effective.mapValues { (_, p) -> PermissionResponse(p.feature, p.canView, p.canEdit, p.canDelete) }
            ))
        }
    }

    route("/permissions/groups") {
        get {
            if (!requirePermission("permissions", "view")) return@get
            val choirId = extractChoirId()
            call.respond(PermissionService.listGroups(choirId).map { it.toResponse() })
        }

        post {
            if (!requirePermission("permissions", "edit")) return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateGroupRequest>()
            val group = PermissionService.createGroup(choirId, req.name.trim())
            call.respond(HttpStatusCode.Created, group.toResponse())
        }

        route("/{id}") {
            delete {
                if (!requirePermission("permissions", "delete")) return@delete
                val id = UUID.fromString(call.parameters["id"])
                val choirId = extractChoirId()
                if (PermissionService.deleteGroup(id, choirId)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot delete the Admin group"))
                }
            }

            put("/permissions") {
                if (!requirePermission("permissions", "edit")) return@put
                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<SetPermissionRequest>()
                PermissionService.setPermission(id, req.feature, req.canView, req.canEdit, req.canDelete)
                call.respond(HttpStatusCode.OK)
            }

            post("/members") {
                if (!requirePermission("permissions", "edit")) return@post
                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<AddMemberRequest>()
                val added = PermissionService.addMember(id, UUID.fromString(req.userId))
                if (added) call.respond(HttpStatusCode.Created) else call.respond(HttpStatusCode.Conflict)
            }

            delete("/members/{userId}") {
                if (!requirePermission("permissions", "edit")) return@delete
                val id = UUID.fromString(call.parameters["id"])
                val userId = UUID.fromString(call.parameters["userId"])
                if (PermissionService.removeMember(id, userId)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
