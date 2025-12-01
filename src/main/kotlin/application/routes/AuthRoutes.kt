package com.myPhysioTime.application.routes

import com.myPhysioTime.application.dto.*
import com.myPhysioTime.domain.models.CreateUserCommand
import com.myPhysioTime.domain.models.User
import com.myPhysioTime.domain.usecases.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(
    loginUseCase: LoginUseCase,
    registerUseCase: RegisterUseCase
) {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()

            loginUseCase.execute(request.email, request.password)
                .onSuccess { token ->
                    call.respond(HttpStatusCode.OK, mapOf("token" to token))
                }
                .onFailure { e ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(e.message ?: "Invalid credentials")
                    )
                }
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val command = request.toCommand()

            registerUseCase.execute(command)
                .onSuccess { user ->
                    call.respond(HttpStatusCode.Created, user.toDTO())
                }
                .onFailure { e ->
                    println("Error en authRoutes: ${e::class.simpleName} - ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(e.message ?: "Registration failed")
                    )
                }
        }
    }
}

fun RegisterRequest.toCommand() = CreateUserCommand(
    firstName = this.firstName,
    lastName = this.lastName,
    email = this.email,
    phone = this.phone,
    password = this.password,
    roleId = 2
)

fun User.toDTO() = UserDTO(
    id = this.id,
    firstName = this.firstName,
    lastName = this.lastName,
    email = this.email,
    phone = this.phone,
    role = this.roleName.name
)