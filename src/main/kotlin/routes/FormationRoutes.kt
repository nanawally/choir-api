package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.FormationService
import service.PlacementDTO
import java.util.*

@Serializable
data class CreateFormationRequest(val name: String)

@Serializable
data class FormationResponse(val id: String, val name: String)

@Serializable
data class PlacementBody(val choristId: String, val gridX: Int, val gridY: Int)

@Serializable
data class FormationDetailResponse(
    val id: String,
    val name: String,
    val placements: List<PlacementBody>,
)

fun Route.formationRoutes() {
    route("/formations") {
        get {
            val formations = FormationService.list().map {
                FormationResponse(it.id.toString(), it.name)
            }
            call.respond(formations)
        }

        post {
            val req = call.receive<CreateFormationRequest>()
            val formation = FormationService.create(req.name)
            call.respond(
                HttpStatusCode.Created,
                FormationResponse(formation.id.toString(), formation.name)
            )
        }

        get("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val result = FormationService.get(id)
            if (result != null) {
                call.respond(FormationDetailResponse(
                    result.id.toString(),
                    result.name,
                    result.placements.map {
                        PlacementBody(it.choristId.toString(), it.gridX, it.gridY)
                    },
                ))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            if (FormationService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/{id}/placements") {
            val id = UUID.fromString(call.parameters["id"])
            val body = call.receive<List<PlacementBody>>()
            FormationService.savePlacements(
                id,
                body.map { PlacementDTO(UUID.fromString(it.choristId), it.gridX, it.gridY) },
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}