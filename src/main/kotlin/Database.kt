import io.ktor.server.application.*
import model.Chorists
import model.ConcertChorists
import model.ConcertSongs
import model.Concerts
import model.Formations
import model.HiddenChorists
import model.Placements
import model.SongFormations
import model.Songs
import model.VoiceAssignments
import model.VoiceGroups
import model.VoiceParts
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val url = environment.config.property("database.url").getString()
    val user = environment.config.property("database.user").getString()
    val password = environment.config.property("database.password").getString()

    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password,
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Chorists, Concerts, ConcertChorists, ConcertSongs, Formations, HiddenChorists, Placements,
            Songs, SongFormations, VoiceGroups, VoiceParts, VoiceAssignments
        )
    }
}
