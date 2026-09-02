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
    route("/concert-songs/{concertSongId}/formations") {

        get {
            val concertSongId = UUID.fromString(call.parameters["concertSongId"])
            val songFormations = SongFormationService.listBySong(concertSongId).map { it.toString() }
            call.respond(songFormations)
        }

        put {
            val concertSongId = UUID.fromString(call.parameters["concertSongId"])
            val body = call.receive<SetSongFormationsRequest>()
            SongFormationService.setBySong(
                concertSongId,
                body.formationIds.map { UUID.fromString(it) }
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
