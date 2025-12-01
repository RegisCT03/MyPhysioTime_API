package com.myPhysioTime.application.plugins

import com.myPhysioTime.infrastructure.security.JwtTokenGenerator
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity(jwtTokenGenerator: JwtTokenGenerator) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "com.myPhysioTime"
            verifier(jwtTokenGenerator.verifier)
            validate { credential ->
                if (credential.payload.getClaim("email").asString().isNotEmpty()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}