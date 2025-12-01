package com.myPhysioTime.application.plugins

import com.myPhysioTime.application.routes.*
import com.myPhysioTime.domain.usecases.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    getAllClientsUseCase: GetAllClientsUseCase,
    getUserByIdUseCase: GetUserByIdUseCase,
    getAllBookingsUseCase: GetAllBookingsUseCase,
    getBookingsByClientUseCase: GetBookingsByClientUseCase,
    createBookingUseCase: CreateBookingUseCase,
    updateBookingUseCase: UpdateBookingUseCase,
    deleteBookingUseCase: DeleteBookingUseCase,
    getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    getBookingsByStateUseCase: GetBookingsByStateUseCase,
    getAllServicesUseCase: GetAllServicesUseCase,
    createServiceUseCase: CreateServiceUseCase,
    updateServiceUseCase: UpdateServiceUseCase,
    deleteServiceUseCase: DeleteServiceUseCase,
    getDashboardStatsUseCase: GetDashboardStatsUseCase,
    createPaymentUseCase: CreatePaymentUseCase,
    getPaymentsByClientUseCase: GetPaymentsByClientUseCase,
    loginUseCase: LoginUseCase,
    registerUseCase: RegisterUseCase
) {
    routing {
        authRoutes(loginUseCase, registerUseCase)
        userRoutes(getAllClientsUseCase, getUserByIdUseCase)
        bookingRoutes(
            getAllBookingsUseCase,
            getBookingsByClientUseCase,
            getBookingsByStateUseCase,
            getDashboardStatsUseCase,
            createBookingUseCase,
            updateBookingUseCase,
            getAvailableSlotsUseCase,
            deleteBookingUseCase
        )
        serviceRoutes(getAllServicesUseCase, createServiceUseCase, updateServiceUseCase, deleteServiceUseCase)
        paymentRoutes(createPaymentUseCase, getPaymentsByClientUseCase)
    }
}