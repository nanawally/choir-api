package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.VoiceGroupService
import java.util.UUID

@Serializable
data class CreateVoiceGroupRequest(val name: String)

@Serializable
data class AddVoicePartRequest(val name: String, val color: String, val shape:
String)

@Serializable
data class AssignRequest(val choristId: String, val voicePartId: String)

@Serializable
data class VoicePartResponse(val id: String, val name: String, val color: String,
                             val shape: String)

@Serializable
data class VoiceGroupResponse(val id: String, val name: String, val parts:
List<VoicePartResponse>)

@Serializable
data class VoiceAssignmentResponse(val choristId: String, val voicePartId: String)

fun Route.voiceGroupRoutes() {
    route("/voice-groups") {
        get {
            val groups = VoiceGroupService.list().map { g ->
                VoiceGroupResponse(
                    g.id.toString(), g.name,
                    g.parts.map { VoicePartResponse(it.id.toString(), it.name, it.color, it.shape) },
                )
            }
            call.respond(groups)
        }

        post {
            val req = call.receive<CreateVoiceGroupRequest>()
            val group = VoiceGroupService.create(req.name)
            call.respond(
                HttpStatusCode.Created,
                VoiceGroupResponse(group.id.toString(), group.name, emptyList()),
            )
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            if (VoiceGroupService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/{id}/parts") {
            val groupId = UUID.fromString(call.parameters["id"])
            val req = call.receive<AddVoicePartRequest>()
            val part = VoiceGroupService.addPart(groupId, req.name, req.color, req.shape)
            call.respond(
                HttpStatusCode.Created,
                VoicePartResponse(part.id.toString(), part.name, part.color, part.shape),
            )
        }

        delete("/parts/{partId}") {
            val partId = UUID.fromString(call.parameters["partId"])
            if (VoiceGroupService.deletePart(partId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/{id}/assignments") {
            val groupId = UUID.fromString(call.parameters["id"])
            val assignments = VoiceGroupService.getAssignments(groupId).map {
                VoiceAssignmentResponse(it.choristId.toString(), it.voicePartId.toString())
            }
            call.respond(assignments)
        }

        post("/assignments") {
            val req = call.receive<AssignRequest>()
            VoiceGroupService.assign(UUID.fromString(req.choristId),
                UUID.fromString(req.voicePartId))
            call.respond(HttpStatusCode.OK)
        }

        delete("/{groupId}/assignments/{choristId}") {
            val groupId = UUID.fromString(call.parameters["groupId"])
            val choristId = UUID.fromString(call.parameters["choristId"])
            VoiceGroupService.unassign(choristId, groupId)
            call.respond(HttpStatusCode.OK)
        }
    }
}