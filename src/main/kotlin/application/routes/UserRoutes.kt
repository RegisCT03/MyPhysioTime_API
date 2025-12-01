package com.myPhysioTime.application.routes

import com.myPhysioTime.application.dto.*
import com.myPhysioTime.domain.models.RoleName
import com.myPhysioTime.domain.usecases.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.format.DateTimeFormatter

fun Route.userRoutes(
    getAllClientsUseCase: GetAllClientsUseCase,
    getUserByIdUseCase: GetUserByIdUseCase
) {
    route("/clients") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != RoleName.ADMIN.name) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Access denied")
                    )
                }

                getAllClientsUseCase.execute()
                    .onSuccess { clients ->
                        val dtos = clients.map { client ->
                            ClientStatsDTO(
                                id = client.id,
                                name = client.name,
                                email = client.email,
                                phone = client.phone,
                                totalBookings = client.totalBookings,
                                lastVisit = client.lastVisit?.format(DateTimeFormatter.ISO_DATE_TIME),
                                preferredService = client.preferredService
                            )
                        }
                        call.respond(dtos)
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Error fetching clients")
                        )
                    }
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != RoleName.ADMIN.name) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Access denied")
                    )
                }

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                getUserByIdUseCase.execute(id)
                    .onSuccess { user ->
                        val dto = UserDTO(
                            id = user.id,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            email = user.email,
                            phone = user.phone,
                            role = user.roleName.name
                        )
                        call.respond(dto)
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(e.message ?: "User not found")
                        )
                    }
            }
        }
    }
}