package com.myPhysioTime.domain.ports

import com.myPhysioTime.domain.models.*
import java.time.LocalDate
import java.time.LocalDateTime

interface UserRepository {
    suspend fun findById(id: Int): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findCredentialsByEmail(email: String): UserCredentials?
    suspend fun findAll(): List<User>
    suspend fun findAllClients(): List<ClientStats>
    suspend fun create(command: CreateUserCommand): User
    suspend fun update(id: Int, command: UpdateUserCommand): User?
    suspend fun delete(id: Int): Boolean
    suspend fun updateLastLogin(id: Int): Boolean
}

interface ServiceRepository {
    suspend fun findById(id: Int): Service?
    suspend fun findAll(): List<Service>
    suspend fun findAllActive(): List<Service>
    suspend fun create(command: CreateServiceCommand): Service
    suspend fun update(id: Int, command: UpdateServiceCommand): Service?
    suspend fun delete(id: Int): Boolean
}

interface BookingRepository {
    suspend fun findById(id: Int): BookingDetail?
    suspend fun findAll(): List<BookingDetail>
    suspend fun findByClientId(clientId: Int): List<BookingDetail>
    suspend fun findByDate(date: LocalDate): List<BookingDetail>
    suspend fun findByState(state: BookingState): List<BookingDetail>
    suspend fun findByDateRange(from: LocalDateTime, to: LocalDateTime): List<BookingDetail>
    suspend fun create(command: CreateBookingCommand): Booking
    suspend fun update(id: Int, command: UpdateBookingCommand): Booking?
    suspend fun delete(id: Int): Boolean
    suspend fun getAvailableSlots(date: LocalDate, duration: Int = 60): List<TimeSlot>
    suspend fun getDashboardStats(): DashboardStats
}

interface PaymentRepository {
    suspend fun findById(id: Int): Payment?
    suspend fun findByStripePaymentId(stripePaymentId: String): Payment?
    suspend fun findByClientId(clientId: Int): List<Payment>
    suspend fun create(command: CreatePaymentCommand): Payment
    suspend fun updateStatus(id: Int, status: PaymentStatus): Payment?
}

interface PasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}

interface TokenGenerator {
    fun generate(userId: Int, email: String, role: RoleName): String
    fun verify(token: String): TokenPayload?
}

data class TokenPayload(
    val userId: Int,
    val email: String,
    val role: RoleName
)