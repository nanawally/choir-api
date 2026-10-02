import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    val url = environment.config.property("database.url").getString()
    val user = environment.config.property("database.user").getString()
    val password = environment.config.property("database.password").getString()

    Flyway.configure()
        .dataSource(url, user, password)
        .baselineOnMigrate(true)   // stamps existing DB at V1 without running the migration
        .baselineVersion("1")
        .load()
        .migrate()

    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password,
    )
}
