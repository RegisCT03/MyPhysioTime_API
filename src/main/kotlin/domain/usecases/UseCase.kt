package com.myPhysioTime.domain.usecases

import com.myPhysioTime.domain.ports.*
import com.myPhysioTime.domain.models.*
import java.time.LocalDate

class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenGenerator: TokenGenerator
) {
    suspend fun execute(email: String, password: String): Result<String> {
        val credentials = userRepository.findCredentialsByEmail(email)
            ?: return Result.failure(Exception("Invalid credentials"))

        if (!passwordHasher.verify(password, credentials.password)) {
            return Result.failure(Exception("Invalid credentials"))
        }

        userRepository.updateLastLogin(credentials.id)
        val token = tokenGenerator.generate(credentials.id, credentials.email, credentials.roleName)

        return Result.success(token)
    }
}

class RegisterUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher
) {
    suspend fun execute(command: CreateUserCommand): Result<User> {
        if (userRepository.findByEmail(command.email) != null) {
            return Result.failure(Exception("Email already exists"))
        }

        val hashedPassword = passwordHasher.hash(command.password)
        val commandWithHashedPassword = command.copy(password = hashedPassword)

        return try {
            val user = userRepository.create(commandWithHashedPassword)
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace() // imprime todo el stacktrace
            val message = when {
                e.message?.contains("duplicate key value", ignoreCase = true) == true ->
                    "Email already exists"
                e.message?.contains("role_id", ignoreCase = true) == true ->
                    "Role not found"
                e.message?.contains("phone", ignoreCase = true) == true ->
                    "Phone number too long (max 10 digits)"
                else -> "Unexpected database error: ${e::class.simpleName} - ${e.cause?.message ?: e.message}"
            }
            return Result.failure(Exception(message))
        }
    }
}

class GetAllClientsUseCase(private val userRepository: UserRepository) {
    suspend fun execute(): Result<List<ClientStats>> {
        return try {
            val clients = userRepository.findAllClients()
            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetUserByIdUseCase(private val userRepository: UserRepository) {
    suspend fun execute(id: Int): Result<User> {
        val user = userRepository.findById(id)
            ?: return Result.failure(Exception("User not found"))
        return Result.success(user)
    }
}

class GetAllServicesUseCase(private val serviceRepository: ServiceRepository) {
    suspend fun execute(activeOnly: Boolean = false): Result<List<Service>> {
        return try {
            val services = if (activeOnly) {
                serviceRepository.findAllActive()
            } else {
                serviceRepository.findAll()
            }
            Result.success(services)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CreateServiceUseCase(private val serviceRepository: ServiceRepository) {
    suspend fun execute(command: CreateServiceCommand): Result<Service> {
        if (command.price < 0) {
            return Result.failure(Exception("Price must be positive"))
        }
        if (command.duration <= 0) {
            return Result.failure(Exception("Duration must be positive"))
        }

        return try {
            val service = serviceRepository.create(command)
            Result.success(service)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateServiceUseCase(private val serviceRepository: ServiceRepository) {
    suspend fun execute(id: Int, command: UpdateServiceCommand): Result<Service> {
        return try {
            val service = serviceRepository.update(id, command)
                ?: return Result.failure(Exception("Service not found"))
            Result.success(service)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteServiceUseCase(private val serviceRepository: ServiceRepository) {
    suspend fun execute(id: Int): Result<Boolean> {
        return try {
            val deleted = serviceRepository.delete(id)
            if (!deleted) {
                Result.failure(Exception("Service not found"))
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAllBookingsUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(): Result<List<BookingDetail>> {
        return try {
            val bookings = bookingRepository.findAll()
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetBookingsByClientUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(clientId: Int): Result<List<BookingDetail>> {
        return try {
            val bookings = bookingRepository.findByClientId(clientId)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetBookingsByStateUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(state: BookingState): Result<List<BookingDetail>> {
        return try {
            val bookings = bookingRepository.findByState(state)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetDashboardStatsUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(): Result<DashboardStats> {
        return try {
            val stats = bookingRepository.getDashboardStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CreateBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val serviceRepository: ServiceRepository,
    private val userRepository: UserRepository
) {
    suspend fun execute(command: CreateBookingCommand): Result<Booking> {
        // Verificar que el servicio existe
        serviceRepository.findById(command.serviceId)
            ?: return Result.failure(Exception("Service not found"))

        // Verificar que el cliente existe
        userRepository.findById(command.clientId)
            ?: return Result.failure(Exception("Client not found"))

        return try {
            val booking = bookingRepository.create(command)
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateBookingUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(id: Int, command: UpdateBookingCommand): Result<Booking> {
        return try {
            val booking = bookingRepository.update(id, command)
                ?: return Result.failure(Exception("Booking not found"))
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteBookingUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(id: Int): Result<Boolean> {
        return try {
            val deleted = bookingRepository.delete(id)
            if (!deleted) {
                Result.failure(Exception("Booking not found"))
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAvailableSlotsUseCase(private val bookingRepository: BookingRepository) {
    suspend fun execute(date: LocalDate, duration: Int = 60): Result<List<TimeSlot>> {
        return try {
            val slots = bookingRepository.getAvailableSlots(date, duration)
            Result.success(slots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CreatePaymentUseCase(private val paymentRepository: PaymentRepository) {
    suspend fun execute(command: CreatePaymentCommand): Result<Payment> {
        return try {
            val payment = paymentRepository.create(command)
            Result.success(payment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetPaymentsByClientUseCase(private val paymentRepository: PaymentRepository) {
    suspend fun execute(clientId: Int): Result<List<Payment>> {
        return try {
            val payments = paymentRepository.findByClientId(clientId)
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}