package com.myPhysioTime.infrastructure.database.repositories

import com.myPhysioTime.domain.models.*
import com.myPhysioTime.domain.ports.ServiceRepository
import com.myPhysioTime.infrastructure.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class ServiceRepositoryImpl : ServiceRepository {

    override suspend fun findById(id: Int): Service? = DatabaseFactory.dbQuery {
        ServicesTable
            .select { ServicesTable.id eq id }
            .mapNotNull { it.toService() }
            .singleOrNull()
    }

    override suspend fun findAll(): List<Service> = DatabaseFactory.dbQuery {
        ServicesTable
            .selectAll()
            .mapNotNull { it.toService() }
    }

    override suspend fun findAllActive(): List<Service> = DatabaseFactory.dbQuery {
        ServicesTable
            .select { ServicesTable.isActive eq true }
            .mapNotNull { it.toService() }
    }

    override suspend fun create(command: CreateServiceCommand): Service = DatabaseFactory.dbQuery {
        val now = LocalDateTime.now()
        val id = ServicesTable.insert {
            it[name] = command.name
            it[description] = command.description
            it[price] = command.price.toBigDecimal()
            it[duration] = command.duration
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        } get ServicesTable.id

        findById(id.value)!!
    }

    override suspend fun update(id: Int, command: UpdateServiceCommand): Service? = DatabaseFactory.dbQuery {
        val updated = ServicesTable.update({ ServicesTable.id eq id }) {
            command.name?.let { name -> it[ServicesTable.name] = name }
            command.description?.let { desc -> it[description] = desc }
            command.price?.let { price -> it[ServicesTable.price] = price.toBigDecimal() }
            command.duration?.let { dur -> it[duration] = dur }
            command.isActive?.let { active -> it[isActive] = active }
            it[updatedAt] = LocalDateTime.now()
        }

        if (updated > 0) findById(id) else null
    }

    override suspend fun delete(id: Int): Boolean = DatabaseFactory.dbQuery {
        ServicesTable.deleteWhere { ServicesTable.id eq id } > 0
    }
}

private fun ResultRow.toService() = Service(
    id = this[ServicesTable.id].value,
    name = this[ServicesTable.name],
    description = this[ServicesTable.description],
    price = this[ServicesTable.price].toDouble(),
    duration = this[ServicesTable.duration],
    stripeId = this[ServicesTable.stripeId],
    isActive = this[ServicesTable.isActive],
    createdAt = this[ServicesTable.createdAt]
)