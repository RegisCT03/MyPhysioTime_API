package com.myPhysioTime.application.routes

import com.myPhysioTime.application.dto.*
import com.myPhysioTime.domain.models.*
import com.myPhysioTime.domain.usecases.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.serviceRoutes(
    getAllServicesUseCase: GetAllServicesUseCase,
    createServiceUseCase: CreateServiceUseCase,
    updateServiceUseCase: UpdateServiceUseCase,
    deleteServiceUseCase: DeleteServiceUseCase
) {
    route("/services") {
        // Público - listar servicios activos
        get {
            val activeOnly = call.request.queryParameters["active"]?.toBoolean() ?: false

            getAllServicesUseCase.execute(activeOnly)
                .onSuccess { services ->
                    call.respond(services.map { it.toDTO() }) // ✅ ahora sí compila
                }
                .onFailure { e ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(e.message ?: "Error fetching services")
                    )
                }
        }

        authenticate("auth-jwt") {
            // Admin - crear servicio
            post {
                val request = call.receive<CreateServiceRequest>()
                val command = request.toCommand()

                createServiceUseCase.execute(command)
                    .onSuccess { service ->
                        call.respond(HttpStatusCode.Created, service.toDTO())
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(e.message ?: "Error creating service")
                        )
                    }
            }

            // Admin - actualizar servicio
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

                val request = call.receive<UpdateServiceRequest>()
                val command = request.toCommand()

                updateServiceUseCase.execute(id, command)
                    .onSuccess { service ->
                        call.respond(service.toDTO())
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(e.message ?: "Service not found")
                        )
                    }
            }

            // Admin - eliminar servicio
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

                deleteServiceUseCase.execute(id)
                    .onSuccess {
                        call.respond(HttpStatusCode.NoContent)
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(e.message ?: "Service not found")
                        )
                    }
            }
        }
    }
}

// ✅ Extensiones movidas fuera de serviceRoutes
fun Service.toDTO() = ServiceDTO(
    id = id,
    name = name,
    description = description,
    price = price,
    duration = duration,
    isActive = isActive
)

fun CreateServiceRequest.toCommand() = CreateServiceCommand(
    name = name,
    description = description,
    price = price,
    duration = duration
)

fun UpdateServiceRequest.toCommand() = UpdateServiceCommand(
    name = name,
    description = description,
    price = price,
    duration = duration,
    isActive = isActive
)