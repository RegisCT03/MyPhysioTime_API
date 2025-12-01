package com.myPhysioTime.infrastructure.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object RolesTable : IntIdTable("roles") {
    val name = varchar("name", 50).uniqueIndex()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object UsersTable : IntIdTable("users") {
    val roleId = reference("role_id", RolesTable)
    val stripeId = varchar("stripe_id", 255).nullable().uniqueIndex()
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 10).nullable()
    val password = varchar("password", 255)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val lastLogin = datetime("last_login").nullable()
}

object ServicesTable : IntIdTable("services") {
    val name = varchar("name", 255)
    val description = varchar("description", 1000).nullable()
    val price = decimal("price", 10, 2)
    val duration = integer("duration") // minutos
    val stripeId = varchar("stripe_id", 255).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object BookingsTable : IntIdTable("bookings") {
    val serviceId = reference("service_id", ServicesTable)
    val clientId = reference("client_id", UsersTable)
    val physiotherapeutId = reference("physiotherapeut_id", UsersTable).nullable()
    val date = datetime("date")
    val state = varchar("state", 20) // BookingState enum
    val notes = varchar("notes", 1000).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object PaymentsTable : IntIdTable("payments") {
    val stripePaymentId = varchar("stripe_payment_id", 255).uniqueIndex()
    val serviceId = reference("service_id", ServicesTable)
    val clientId = reference("client_id", UsersTable)
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("MXN")
    val status = varchar("status", 50)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}