package com.myPhysioTime.application.routes

import com.myPhysioTime.application.dto.*
import com.myPhysioTime.domain.models.CreatePaymentCommand
import com.myPhysioTime.domain.usecases.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.paymentRoutes(
    createPaymentUseCase: CreatePaymentUseCase,
    getPaymentsByClientUseCase: GetPaymentsByClientUseCase
) {
    route("/payments") {
        authenticate("auth-jwt") {
            // Crear pago
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreatePaymentRequest>()
                val command = CreatePaymentCommand(
                    stripePaymentId = request.stripePaymentId,
                    serviceId = request.serviceId,
                    clientId = userId,
                    amount = request.amount,
                    currency = request.currency
                )

                createPaymentUseCase.execute(command)
                    .onSuccess { payment ->
                        call.respond(HttpStatusCode.Created, payment)
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(e.message ?: "Error creating payment")
                        )
                    }
            }

            // Ver mis pagos
            get("/my") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                getPaymentsByClientUseCase.execute(userId)
                    .onSuccess { payments ->
                        call.respond(payments)
                    }
                    .onFailure { e ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(e.message ?: "Error fetching payments")
                        )
                    }
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class CreatePaymentRequest(
    val stripePaymentId: String,
    val serviceId: Int,
    val amount: Double,
    val currency: String = "MXN"
)