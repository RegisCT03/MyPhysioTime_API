package com.myPhysioTime

import com.myPhysioTime.application.plugins.*
import com.myPhysioTime.domain.ports.*
import com.myPhysioTime.domain.usecases.*
import com.myPhysioTime.infrastructure.database.DatabaseFactory
import com.myPhysioTime.infrastructure.database.repositories.*
import com.myPhysioTime.infrastructure.security.*
import com.myPhysioTime.domain.usecases.GetBookingsByStateUseCase
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.github.cdimascio.dotenv.dotenv

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val dotenv = dotenv {
        directory = "./"
        ignoreIfMissing = false
    }

    fun getEnv(key: String): String =
        dotenv[key] ?: System.getenv(key) ?: error("$key not set")

    val dbUrl = getEnv("DATABASE_URL")
    val dbUser = getEnv("DATABASE_USER")
    val dbPassword = getEnv("DATABASE_PASSWORD")
    val jwtSecret = getEnv("JWT_SECRET")
    val allowedHosts = getEnv("ALLOWED_HOSTS").split(",")

    //base de datos
    DatabaseFactory.init(dbUrl, dbUser, dbPassword)

    //infraestructura
    val bookingRepository: BookingRepository = BookingRepositoryImpl()
    val userRepository: UserRepository = UserRepositoryImpl()
    val serviceRepository: ServiceRepository = ServiceRepositoryImpl()
    val paymentRepository: PaymentRepository = PaymentRepositoryImpl()
    val getBookingsByStateUseCase = GetBookingsByStateUseCase(bookingRepository)

    val passwordHasher: PasswordHasher = BcryptPasswordHasher()
    val tokenGenerator = JwtTokenGenerator(
        secret = jwtSecret,
        issuer = "myPhysioTime",
        audience = "myPhysioTime-users",
        expirationTime = 3600000
    )

    configureSerialization()

    //seguridad y CORS
    configureSecurity(tokenGenerator)
    configureCORS(allowedHosts)

    //casos de uso
    val getAllClientsUseCase = GetAllClientsUseCase(userRepository)
    val getUserByIdUseCase = GetUserByIdUseCase(userRepository)

    val getAllBookingsUseCase = GetAllBookingsUseCase(bookingRepository)
    val getBookingsByClientUseCase = GetBookingsByClientUseCase(bookingRepository)
    val createBookingUseCase = CreateBookingUseCase(bookingRepository, serviceRepository, userRepository)
    val updateBookingUseCase = UpdateBookingUseCase(bookingRepository)
    val deleteBookingUseCase = DeleteBookingUseCase(bookingRepository)
    val getAvailableSlotsUseCase = GetAvailableSlotsUseCase(bookingRepository)

    val getAllServicesUseCase = GetAllServicesUseCase(serviceRepository)
    val createServiceUseCase = CreateServiceUseCase(serviceRepository)
    val updateServiceUseCase = UpdateServiceUseCase(serviceRepository)
    val deleteServiceUseCase = DeleteServiceUseCase(serviceRepository)

    val getDashboardStatsUseCase = GetDashboardStatsUseCase(bookingRepository)

    val createPaymentUseCase = CreatePaymentUseCase(paymentRepository)
    val getPaymentsByClientUseCase = GetPaymentsByClientUseCase(paymentRepository)

    val loginUseCase = LoginUseCase(userRepository, passwordHasher, tokenGenerator)
    val registerUseCase = RegisterUseCase(userRepository, passwordHasher)

    //rutas
    configureRouting(
        getAllClientsUseCase,
        getUserByIdUseCase,
        getAllBookingsUseCase,
        getBookingsByClientUseCase,
        createBookingUseCase,
        updateBookingUseCase,
        deleteBookingUseCase,
        getAvailableSlotsUseCase,
        getBookingsByStateUseCase,
        getAllServicesUseCase,
        createServiceUseCase,
        updateServiceUseCase,
        deleteServiceUseCase,
        getDashboardStatsUseCase,
        createPaymentUseCase,
        getPaymentsByClientUseCase,
        loginUseCase,
        registerUseCase,
    )
}