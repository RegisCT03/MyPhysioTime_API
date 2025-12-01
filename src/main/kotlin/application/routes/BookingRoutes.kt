package com.myPhysioTime.application.routes


import com.myPhysioTime.application.dto.*
import com.myPhysioTime.domain.models.BookingState
import com.myPhysioTime.domain.usecases.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

fun Route.bookingRoutes(
    getAllBookingsUseCase: GetAllBookingsUseCase,
    getBookingsByClientUseCase: GetBookingsByClientUseCase,
    getBookingsByStateUseCase: GetBookingsByStateUseCase,
    getDashboardStatsUseCase: GetDashboardStatsUseCase,
    createBookingUseCase: CreateBookingUseCase,
    updateBookingUseCase: UpdateBookingUseCase,
    getAvailableSlotsUseCase: GetAvailableSlotsUseCase
) {
    route("/bookings") {
        authenticate("auth-jwt") {

            // 📌 Listar todas las reservas (solo admin)
            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "ADMIN") {
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                }

                getAllBookingsUseCase.execute()
                    .onSuccess { bookings -> call.respond(bookings.map { it.toDTO() }) }
                    .onFailure { e -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Error fetching bookings")) }
            }

            // 📌 Listar reservas de un cliente autenticado
            get("/my") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                getBookingsByClientUseCase.execute(userId)
                    .onSuccess { bookings -> call.respond(bookings.map { it.toDTO() }) }
                    .onFailure { e -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Error fetching client bookings")) }
            }

            // 📌 Filtrar reservas por estado
            get("/state/{state}") {
                val stateParam = call.parameters["state"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("State parameter required"))

                val state = try {
                    BookingState.valueOf(stateParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid booking state"))
                }

                getBookingsByStateUseCase.execute(state)
                    .onSuccess { bookings -> call.respond(bookings.map { it.toDTO() }) }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Error fetching bookings by state")
                        )
                    }
            }

            // 📌 Estadísticas del dashboard (solo admin)
            get("/dashboard/stats") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != "ADMIN") {
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                }

                getDashboardStatsUseCase.execute()
                    .onSuccess { stats -> call.respond(stats) }
                    .onFailure { e -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Error fetching dashboard stats")) }
            }

            // 📌 Crear reserva
            post {
                val principal = call.principal<JWTPrincipal>()
                val clientId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateBookingRequest>()
                val command = request.toCommand(clientId)

                createBookingUseCase.execute(command)
                    .onSuccess { booking -> call.respond(HttpStatusCode.Created, booking.toDTO()) }
                    .onFailure { e -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error creating booking")) }
            }

            // 📌 Actualizar reserva
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid booking id"))

                val request = call.receive<UpdateBookingRequest>()
                val command = request.toCommand()

                updateBookingUseCase.execute(id, command)
                    .onSuccess { booking -> call.respond(booking.toDTO()) }
                    .onFailure { e -> call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Booking not found")) }
            }

            // 📌 Consultar slots disponibles
            get("/slots") {
                val dateParam = call.request.queryParameters["date"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Date parameter required"))

                val date = try { LocalDate.parse(dateParam)
                } catch (e: Exception) {
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date format. Expected yyyy-MM-dd"))
                }

                getAvailableSlotsUseCase.execute(date)
                    .onSuccess { slots -> call.respond(slots.map { it.toDTO() }) }
                    .onFailure { e -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Error fetching available slots")) }
            }
        }
    }
}