package routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.SongFormationService
import java.util.*

@Serializable
data class SetSongFormationsRequest(val formationIds: List<String>)

fun Route.songFormationRoutes() {
    route("/songs/{songId}/formations") {

        get {
            val songId = UUID.fromString(call.parameters["songId"])
            val songFormations = SongFormationService.listBySong(songId).map { it.toString() }
            call.respond(songFormations)
        }

        put {
            val songId = UUID.fromString(call.parameters["songId"])
            val body = call.receive<SetSongFormationsRequest>()
            SongFormationService.setBySong(
                songId,
                body.formationIds.map { UUID.fromString(it) }
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
