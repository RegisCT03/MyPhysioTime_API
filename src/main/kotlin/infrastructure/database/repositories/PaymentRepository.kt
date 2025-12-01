package com.myPhysioTime.infrastructure.database.repositories

import com.myPhysioTime.domain.models.*
import com.myPhysioTime.domain.ports.PaymentRepository
import com.myPhysioTime.infrastructure.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class PaymentRepositoryImpl : PaymentRepository {

    override suspend fun findById(id: Int): Payment? = DatabaseFactory.dbQuery {
        PaymentsTable
            .select { PaymentsTable.id eq id }
            .mapNotNull { it.toPayment() }
            .singleOrNull()
    }

    override suspend fun findByStripePaymentId(stripePaymentId: String): Payment? = DatabaseFactory.dbQuery {
        PaymentsTable
            .select { PaymentsTable.stripePaymentId eq stripePaymentId }
            .mapNotNull { it.toPayment() }
            .singleOrNull()
    }

    override suspend fun findByClientId(clientId: Int): List<Payment> = DatabaseFactory.dbQuery {
        PaymentsTable
            .select { PaymentsTable.clientId eq clientId }
            .mapNotNull { it.toPayment() }
    }

    override suspend fun create(command: CreatePaymentCommand): Payment = DatabaseFactory.dbQuery {
        val now = LocalDateTime.now()
        val id = PaymentsTable.insert {
            it[stripePaymentId] = command.stripePaymentId
            it[serviceId] = command.serviceId
            it[clientId] = command.clientId
            it[amount] = command.amount.toBigDecimal()
            it[currency] = command.currency
            it[status] = PaymentStatus.PENDING.name.lowercase()
            it[createdAt] = now
            it[updatedAt] = now
        } get PaymentsTable.id

        findById(id.value)!!
    }

    override suspend fun updateStatus(id: Int, status: PaymentStatus): Payment? = DatabaseFactory.dbQuery {
        val updated = PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[PaymentsTable.status] = status.name.lowercase()
            it[updatedAt] = LocalDateTime.now()
        }

        if (updated > 0) findById(id) else null
    }
}

private fun ResultRow.toPayment() = Payment(
    id = this[PaymentsTable.id].value,
    stripePaymentId = this[PaymentsTable.stripePaymentId],
    serviceId = this[PaymentsTable.serviceId].value,
    clientId = this[PaymentsTable.clientId].value,
    amount = this[PaymentsTable.amount].toDouble(),
    currency = this[PaymentsTable.currency],
    status = PaymentStatus.valueOf(this[PaymentsTable.status].uppercase()),
    createdAt = this[PaymentsTable.createdAt]
)