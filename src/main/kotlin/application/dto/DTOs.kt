package com.myPhysioTime.application.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDTO
)

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val password: String
)

@Serializable
data class UserDTO(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val role: String
)

@Serializable
data class ServiceDTO(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val duration: Int,
    val isActive: Boolean
)

@Serializable
data class CreateServiceRequest(
    val name: String,
    val description: String?,
    val price: Double,
    val duration: Int
)

@Serializable
data class UpdateServiceRequest(
    val name: String?,
    val description: String?,
    val price: Double?,
    val duration: Int?,
    val isActive: Boolean?
)

@Serializable
data class BookingDTO(
    val id: Int,
    val date: String, // ISO 8601
    val state: String,
    val notes: String?,
    val service: ServiceInfoDTO,
    val client: ClientInfoDTO,
    val physiotherapeut: PhysiotherapeutInfoDTO?,
    val createdAt: String
)

@Serializable
data class ServiceInfoDTO(
    val id: Int,
    val name: String,
    val price: Double,
    val duration: Int
)

@Serializable
data class ClientInfoDTO(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?
)

@Serializable
data class PhysiotherapeutInfoDTO(
    val id: Int,
    val name: String
)

@Serializable
data class CreateBookingRequest(
    val serviceId: Int,
    val clientId: Int,
    val date: String, // ISO 8601
    val notes: String?
)

@Serializable
data class UpdateBookingRequest(
    val state: String?,
    val notes: String?,
    val physiotherapeutId: Int?
)

@Serializable
data class ClientStatsDTO(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val totalBookings: Int,
    val lastVisit: String?, // ISO 8601
    val preferredService: String?
)

@Serializable
data class DashboardStatsDTO(
    val totalBookingsToday: Int,
    val pendingBookings: Int,
    val completedBookings: Int,
    val totalClients: Int
)

@Serializable
data class TimeSlotDTO(
    val time: String,
    val isAvailable: Boolean
)

@Serializable
data class ErrorResponse(
    val message: String
)