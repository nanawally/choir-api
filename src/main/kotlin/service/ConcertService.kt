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
        if (songIds.isNotEmpty()) {
            HiddenChorists.deleteWhere { HiddenChorists.concertSongId inList songIds }
            SongFormations.deleteWhere { SongFormations.concertSongId inList songIds }
        }
        ConcertSongs.deleteWhere { ConcertSongs.concertId eq id }

        val formationIds = Formations.selectAll()
            .where { Formations.concertId eq id }
            .map { it[Formations.id] }
        if (formationIds.isNotEmpty()) {
            Placements.deleteWhere { formationId inList formationIds }
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

        duplicateChorists(id, newConcertId)
        val formationIdMap = duplicateFormations(id, newConcertId)
        val concertSongIdMap = duplicateConcertSongs(id, newConcertId)
        duplicateSongFormationsAndHidden(concertSongIdMap, formationIdMap)

        ConcertDTO(newConcertId, newName, original[Concerts.date], original[Concerts.imageUrl])
    }

    private fun duplicateChorists(fromConcertId: UUID, toConcertId: UUID) {
        ConcertChorists.selectAll()
            .where { ConcertChorists.concertId eq fromConcertId }
            .forEach { cc ->
                ConcertChorists.insert {
                    it[concertId] = toConcertId
                    it[choristId] = cc[ConcertChorists.choristId]
                }
            }
    }

    private fun duplicateFormations(fromConcertId: UUID, toConcertId: UUID): Map<UUID, UUID> {
        val idMap = mutableMapOf<UUID, UUID>()
        Formations.selectAll()
            .where { Formations.concertId eq fromConcertId }
            .forEach { f ->
                val oldId = f[Formations.id]
                val newId = Formations.insert {
                    it[concertId] = toConcertId
                    it[name] = f[Formations.name]
                } get Formations.id
                idMap[oldId] = newId

                Placements.selectAll()
                    .where { Placements.formationId eq oldId }
                    .forEach { p ->
                        Placements.insert {
                            it[formationId] = newId
                            it[choristId] = p[Placements.choristId]
                            it[gridX] = p[Placements.gridX]
                            it[gridY] = p[Placements.gridY]
                        }
                    }
            }
        return idMap
    }

    private fun duplicateConcertSongs(fromConcertId: UUID, toConcertId: UUID): Map<UUID, UUID> {
        val idMap = mutableMapOf<UUID, UUID>()
        ConcertSongs.selectAll()
            .where { ConcertSongs.concertId eq fromConcertId }
            .forEach { cs ->
                val newId = ConcertSongs.insert {
                    it[concertId] = toConcertId
                    it[songId] = cs[ConcertSongs.songId]
                    it[sortOrder] = cs[ConcertSongs.sortOrder]
                } get ConcertSongs.id
                idMap[cs[ConcertSongs.id]] = newId
            }
        return idMap
    }

    private fun duplicateSongFormationsAndHidden(concertSongIdMap: Map<UUID, UUID>, formationIdMap: Map<UUID, UUID>) {
        concertSongIdMap.forEach { (oldCsId, newCsId) ->
            SongFormations.selectAll()
                .where { SongFormations.concertSongId eq oldCsId }
                .forEach { sf ->
                    SongFormations.insert {
                        it[concertSongId] = newCsId
                        it[formationId] = formationIdMap[sf[SongFormations.formationId]]!!
                        it[sortOrder] = sf[SongFormations.sortOrder]
                    }
                }

            HiddenChorists.selectAll()
                .where { HiddenChorists.concertSongId eq oldCsId }
                .forEach { hc ->
                    HiddenChorists.insert {
                        it[concertSongId] = newCsId
                        it[choristId] = hc[HiddenChorists.choristId]
                    }
                }
        }
    }
}
