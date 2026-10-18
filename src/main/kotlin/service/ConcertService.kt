package service

import model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class ConcertDTO(val id: UUID, val name: String, val date: String?, val imageUrl: String?)

object ConcertService {

    fun list(choirId: UUID): List<ConcertDTO> = transaction {
        Concerts.selectAll()
            .where { Concerts.choirId eq choirId }
            .map { ConcertDTO(it[Concerts.id], it[Concerts.name], it[Concerts.date], it[Concerts.imageUrl]) }
    }

    fun create(choirId: UUID, name: String, date: String?): ConcertDTO = transaction {
        val id = Concerts.insert {
            it[Concerts.choirId] = choirId
            it[Concerts.name] = name
            it[Concerts.date] = date
        } get Concerts.id

        ConcertDTO(id, name, date, null)
    }

    fun update(id: UUID, choirId: UUID, name: String, date: String?): Boolean = transaction {
        Concerts.update({ (Concerts.id eq id) and (Concerts.choirId eq choirId) }) {
            it[Concerts.name] = name
            it[Concerts.date] = date
        } > 0
    }

    fun delete(id: UUID, choirId: UUID): Boolean = transaction {
        val exists = Concerts.selectAll()
            .where { (Concerts.id eq id) and (Concerts.choirId eq choirId) }
            .count() > 0
        if (!exists) return@transaction false

        val songIds = ConcertSongs.selectAll()
            .where { ConcertSongs.concertId eq id }
            .map { it[ConcertSongs.id] }

        songIds.forEach { sId ->
            HiddenChorists.deleteWhere { HiddenChorists.concertSongId eq sId }
            SongFormations.deleteWhere { SongFormations.concertSongId eq sId }
        }
        ConcertSongs.deleteWhere { ConcertSongs.concertId eq id }

        val formationIds = Formations.selectAll()
            .where { Formations.concertId eq id }
            .map { it[Formations.id] }

        formationIds.forEach { fId ->
            Placements.deleteWhere { formationId eq fId }
        }
        Formations.deleteWhere { concertId eq id }
        ConcertChorists.deleteWhere { concertId eq id }
        Concerts.deleteWhere { (Concerts.id eq id) and (Concerts.choirId eq choirId) } > 0
    }

    fun getImageUrl(id: UUID): String? = transaction {
        Concerts.selectAll().where { Concerts.id eq id }
            .firstOrNull()?.get(Concerts.imageUrl)
    }

    fun setImageUrl(id: UUID, url: String?) = transaction {
        Concerts.update({ Concerts.id eq id }) {
            it[imageUrl] = url
        }
    }

    fun duplicate(id: UUID, choirId: UUID, newName: String): ConcertDTO? = transaction {
        val original = Concerts.selectAll()
            .where { (Concerts.id eq id) and (Concerts.choirId eq choirId) }
            .firstOrNull() ?: return@transaction null

        val newConcertId = Concerts.insert {
            it[Concerts.choirId] = choirId
            it[name] = newName
            it[date] = original[Concerts.date]
            it[imageUrl] = original[Concerts.imageUrl]
        } get Concerts.id

        ConcertChorists.selectAll()
            .where { ConcertChorists.concertId eq id }
            .forEach { cc ->
                ConcertChorists.insert {
                    it[ConcertChorists.concertId] = newConcertId
                    it[ConcertChorists.choristId] = cc[ConcertChorists.choristId]
                }
            }

        val formationIdMap = mutableMapOf<UUID, UUID>()
        Formations.selectAll()
            .where { Formations.concertId eq id }
            .forEach { f ->
                val oldFormationId = f[Formations.id]
                val newFormationId = Formations.insert {
                    it[Formations.concertId] = newConcertId
                    it[Formations.name] = f[Formations.name]
                } get Formations.id
                formationIdMap[oldFormationId] = newFormationId

                Placements.selectAll()
                    .where { Placements.formationId eq oldFormationId }
                    .forEach { p ->
                        Placements.insert {
                            it[Placements.formationId] = newFormationId
                            it[Placements.choristId] = p[Placements.choristId]
                            it[Placements.gridX] = p[Placements.gridX]
                            it[Placements.gridY] = p[Placements.gridY]
                        }
                    }
            }

        val concertSongIdMap = mutableMapOf<UUID, UUID>()
        ConcertSongs.selectAll()
            .where { ConcertSongs.concertId eq id }
            .forEach { cs ->
                val oldConcertSongId = cs[ConcertSongs.id]
                val newConcertSongId = ConcertSongs.insert {
                    it[ConcertSongs.concertId] = newConcertId
                    it[ConcertSongs.songId] = cs[ConcertSongs.songId]
                    it[ConcertSongs.sortOrder] = cs[ConcertSongs.sortOrder]
                } get ConcertSongs.id
                concertSongIdMap[oldConcertSongId] = newConcertSongId
            }

        concertSongIdMap.forEach { (oldCsId, newCsId) ->
            SongFormations.selectAll()
                .where { SongFormations.concertSongId eq oldCsId }
                .forEach { sf ->
                    SongFormations.insert {
                        it[SongFormations.concertSongId] = newCsId
                        it[SongFormations.formationId] = formationIdMap[sf[SongFormations.formationId]]!!
                        it[SongFormations.sortOrder] = sf[SongFormations.sortOrder]
                    }
                }

            HiddenChorists.selectAll()
                .where { HiddenChorists.concertSongId eq oldCsId }
                .forEach { hc ->
                    HiddenChorists.insert {
                        it[HiddenChorists.concertSongId] = newCsId
                        it[HiddenChorists.choristId] = hc[HiddenChorists.choristId]
                    }
                }
        }

        ConcertDTO(newConcertId, newName, original[Concerts.date], original[Concerts.imageUrl])
    }
}
