package service

import model.Chorists
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class ChoristDTO(val id: UUID, val name: String)

object ChoristService {

    fun list(): List<ChoristDTO> = transaction {
        Chorists.selectAll()
            .map { ChoristDTO(it[Chorists.id], it[Chorists.name]) }
    }

    fun create(name: String): ChoristDTO = transaction {
        val id = Chorists.insert {
            it[Chorists.name] = name
        } get Chorists.id

        ChoristDTO(id, name)
    }

    fun rename(id: UUID, newName: String): Boolean = transaction {
        Chorists.update({ Chorists.id eq id }) {
            it[name] = newName
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        Chorists.deleteWhere { Chorists.id eq id } > 0
    }
}
