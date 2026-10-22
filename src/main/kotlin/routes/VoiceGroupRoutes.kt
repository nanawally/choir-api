package routes

import auth.extractChoirId
import auth.requirePermission
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.VoiceGroupService
import java.util.*

@Serializable
data class CreateVoiceGroupRequest(val name: String, val isStandard: Boolean = false)

@Serializable
data class RenameVoiceGroupRequest(val name: String)

@Serializable
data class AddVoicePartRequest(val name: String, val color: String, val shape: String)

@Serializable
data class UpdateVoicePartRequest(val name: String, val color: String, val shape: String)

@Serializable
data class ReorderVoicePartsRequest(val partIds: List<String>)

@Serializable
data class AssignRequest(val choristId: String, val voicePartId: String)

@Serializable
data class VoicePartResponse(val id: String, val name: String, val color: String, val shape: String)

@Serializable
data class VoiceGroupResponse(val id: String, val name: String, val isStandard: Boolean, val parts: List<VoicePartResponse>)

@Serializable
data class VoiceAssignmentResponse(val choristId: String, val voicePartId: String)

fun Route.voiceGroupRoutes() {
    route("/voice-groups") {
        get {
            if (!requirePermission("voice_groups", "view")) return@get
            val choirId = extractChoirId()
            val groups = VoiceGroupService.list(choirId).map { g ->
                VoiceGroupResponse(
                    g.id.toString(), g.name, g.isStandard,
                    g.parts.map { VoicePartResponse(it.id.toString(), it.name, it.color, it.shape) },
                )
            }
            call.respond(groups)
        }

        post {
            if (!requirePermission("voice_groups", "edit")) return@post
            val choirId = extractChoirId()
            val req = call.receive<CreateVoiceGroupRequest>()
            val group = VoiceGroupService.create(choirId, req.name, req.isStandard)
            call.respond(
                HttpStatusCode.Created,
                VoiceGroupResponse(group.id.toString(), group.name, group.isStandard, emptyList()),
            )
        }

        put("/{id}") {
            if (!requirePermission("voice_groups", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val req = call.receive<RenameVoiceGroupRequest>()
            respondOkOrNotFound(VoiceGroupService.rename(id, choirId, req.name))
        }

        put("/{id}/standard") {
            if (!requirePermission("voice_groups", "edit")) return@put
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            val body = call.receive<Map<String, Boolean>>()
            val isStandard = body["isStandard"] ?: false
            respondOkOrNotFound(VoiceGroupService.setStandard(id, choirId, isStandard))
        }

        delete("/{id}") {
            if (!requirePermission("voice_groups", "delete")) return@delete
            val id = UUID.fromString(call.parameters["id"])
            val choirId = extractChoirId()
            respondOkOrNotFound(VoiceGroupService.delete(id, choirId))
        }

        post("/{id}/parts") {
            if (!requirePermission("voice_groups", "edit")) return@post
            val groupId = UUID.fromString(call.parameters["id"])
            val req = call.receive<AddVoicePartRequest>()
            val part = VoiceGroupService.addPart(groupId, req.name, req.color, req.shape)
            call.respond(
                HttpStatusCode.Created,
                VoicePartResponse(part.id.toString(), part.name, part.color, part.shape),
            )
        }

        put("/{id}/parts/reorder") {
            if (!requirePermission("voice_groups", "edit")) return@put
            val req = call.receive<ReorderVoicePartsRequest>()
            VoiceGroupService.reorderParts(req.partIds.map { UUID.fromString(it) })
            call.respond(HttpStatusCode.OK)
        }

        put("/parts/{partId}") {
            if (!requirePermission("voice_groups", "edit")) return@put
            val partId = UUID.fromString(call.parameters["partId"])
            val req = call.receive<UpdateVoicePartRequest>()
            respondOkOrNotFound(VoiceGroupService.updatePart(partId, req.name, req.color, req.shape))
        }

        delete("/parts/{partId}") {
            if (!requirePermission("voice_groups", "delete")) return@delete
            val partId = UUID.fromString(call.parameters["partId"])
            respondOkOrNotFound(VoiceGroupService.deletePart(partId))
        }

        get("/{id}/assignments") {
            if (!requirePermission("voice_groups", "view")) return@get
            val groupId = UUID.fromString(call.parameters["id"])
            val assignments = VoiceGroupService.getAssignments(groupId).map {
                VoiceAssignmentResponse(it.choristId.toString(), it.voicePartId.toString())
            }
            call.respond(assignments)
        }

        post("/assignments") {
            if (!requirePermission("voice_groups", "edit")) return@post
            val req = call.receive<AssignRequest>()
            VoiceGroupService.assign(UUID.fromString(req.choristId), UUID.fromString(req.voicePartId))
            call.respond(HttpStatusCode.OK)
        }

        delete("/{groupId}/assignments/{choristId}") {
            if (!requirePermission("voice_groups", "edit")) return@delete
            val groupId = UUID.fromString(call.parameters["groupId"])
            val choristId = UUID.fromString(call.parameters["choristId"])
            VoiceGroupService.unassign(choristId, groupId)
            call.respond(HttpStatusCode.OK)
        }
    }
}
