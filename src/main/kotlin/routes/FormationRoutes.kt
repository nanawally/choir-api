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
data class FormationResponse(val id: String, val name: String, val sortOrder: Int)

@Serializable
data class PlacementBody(val choristId: String, val gridX: Int, val gridY: Int)

@Serializable
data class FormationDetailResponse(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val placements: List<PlacementBody>,
    val hiddenChoristIds: List<String>,
)

@Serializable
data class HiddenChoristsBody(val choristIds: List<String>)

@Serializable
data class ReorderBody(val formationIds: List<String>)

@Serializable
data class CopyToConcertBody(val targetConcertId: String)

fun Route.formationRoutes() {
    route("/concerts/{concertId}/formations") {
        get {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val formations = FormationService.listByConcert(concertId).map {
                FormationResponse(it.id.toString(), it.name, it.sortOrder)
            }
            call.respond(formations)
        }

        post {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val req = call.receive<CreateFormationRequest>()
            val formation = FormationService.create(concertId, req.name)
            call.respond(
                HttpStatusCode.Created,
                FormationResponse(formation.id.toString(), formation.name, formation.sortOrder)
            )
        }

        put("/reorder") {
            val concertId = UUID.fromString(call.parameters["concertId"])
            val body = call.receive<ReorderBody>()
            FormationService.reorder(concertId, body.formationIds.map { UUID.fromString(it) })
            call.respond(HttpStatusCode.OK)
        }
    }

    route("/formations/{id}") {
        get {
            val id = UUID.fromString(call.parameters["id"])
            val result = FormationService.get(id)
            if (result != null) {
                call.respond(FormationDetailResponse(
                    result.id.toString(),
                    result.name,
                    result.sortOrder,
                    result.placements.map {
                        PlacementBody(it.choristId.toString(), it.gridX, it.gridY)
                    },
                    result.hiddenChoristIds.map { it.toString() },
                ))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete {
            val id = UUID.fromString(call.parameters["id"])
            if (FormationService.delete(id)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/placements") {
            val id = UUID.fromString(call.parameters["id"])
            val body = call.receive<List<PlacementBody>>()
            FormationService.savePlacements(
                id,
                body.map { PlacementDTO(UUID.fromString(it.choristId), it.gridX, it.gridY) },
            )
            call.respond(HttpStatusCode.OK)
        }

        put("/hidden") {
            val id = UUID.fromString(call.parameters["id"])
            val body = call.receive<HiddenChoristsBody>()
            FormationService.setHiddenChorists(id, body.choristIds.map { UUID.fromString(it) })
            call.respond(HttpStatusCode.OK)
        }

        post("/duplicate") {
            val id = UUID.fromString(call.parameters["id"])
            val result = FormationService.duplicate(id)
            if (result != null) {
                call.respond(
                    HttpStatusCode.Created,
                    FormationResponse(result.id.toString(), result.name, result.sortOrder)
                )
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/copy") {
            val id = UUID.fromString(call.parameters["id"])
            val body = call.receive<CopyToConcertBody>()
            val result = FormationService.copyToConcert(id, UUID.fromString(body.targetConcertId))
            if (result != null) {
                call.respond(
                    HttpStatusCode.Created,
                    FormationResponse(result.id.toString(), result.name, result.sortOrder)
                )
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
