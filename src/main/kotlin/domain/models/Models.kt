package com.myPhysioTime.domain.models

import java.time.LocalDateTime

enum class RoleName {
    ADMIN, CLIENT
}

enum class BookingState {
    PENDING, CONFIRMED, COMPLETED, CANCELLED
}

enum class PaymentStatus {
    PENDING, SUCCEEDED, FAILED, REFUNDED
}

data class Role(
    val id: Int,
    val name: RoleName
)

data class User(
    val id: Int,
    val roleId: Int,
    val roleName: RoleName,
    val stripeId: String?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val createdAt: LocalDateTime,
    val lastLogin: LocalDateTime?
) {
    val fullName: String get() = "$firstName $lastName"
}

data class UserCredentials(
    val id: Int,
    val email: String,
    val password: String,
    val roleId: Int,
    val roleName: RoleName
)

data class Service(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val duration: Int,
    val stripeId: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)

data class Booking(
    val id: Int,
    val serviceId: Int,
    val clientId: Int,
    val physiotherapeutId: Int?,
    val date: LocalDateTime,
    val state: BookingState,
    val notes: String?,
    val createdAt: LocalDateTime
)

data class BookingDetail(
    val id: Int,
    val date: LocalDateTime,
    val state: BookingState,
    val notes: String?,
    val service: ServiceInfo,
    val client: ClientInfo,
    val physiotherapeut: PhysiotherapeutInfo?,
    val createdAt: LocalDateTime
)

data class ServiceInfo(
    val id: Int,
    val name: String,
    val price: Double,
    val duration: Int
)

data class ClientInfo(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?
)

data class PhysiotherapeutInfo(
    val id: Int,
    val name: String
)

data class ClientStats(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val totalBookings: Int,
    val lastVisit: LocalDateTime?,
    val preferredService: String?
)

data class Payment(
    val id: Int,
    val stripePaymentId: String,
    val serviceId: Int,
    val clientId: Int,
    val amount: Double,
    val currency: String,
    val status: PaymentStatus,
    val createdAt: LocalDateTime
)

data class TimeSlot(
    val time: String,
    val isAvailable: Boolean
)

data class DashboardStats(
    val totalBookingsToday: Int,
    val pendingBookings: Int,
    val completedBookings: Int,
    val totalClients: Int
)

data class CreateUserCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val password: String,
    val roleId: Int = 2
)

data class UpdateUserCommand(
    val firstName: String?,
    val lastName: String?,
    val phone: String?
)

data class CreateServiceCommand(
    val name: String,
    val description: String?,
    val price: Double,
    val duration: Int
)

data class UpdateServiceCommand(
    val name: String?,
    val description: String?,
    val price: Double?,
    val duration: Int?,
    val isActive: Boolean?
)

data class CreateBookingCommand(
    val serviceId: Int,
    val clientId: Int,
    val date: LocalDateTime,
    val notes: String?
)

data class UpdateBookingCommand(
    val state: BookingState?,
    val notes: String?,
    val physiotherapeutId: Int?
)

data class CreatePaymentCommand(
    val stripePaymentId: String,
    val serviceId: Int,
    val clientId: Int,
    val amount: Double,
    val currency: String = "MXN"
)