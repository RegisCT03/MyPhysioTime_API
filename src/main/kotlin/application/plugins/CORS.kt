package com.myPhysioTime.application.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS(allowedHosts: List<String>) {
    install(CORS) {
        //métodos HTTP
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        //headers necesarios
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.AccessControlAllowHeaders)

        //headers en la respuesta
        exposeHeader(HttpHeaders.Authorization)

        //credenciales
        allowCredentials = true

        //hosts permitidos
        allowedHosts.forEach { host ->
            val cleanHost = host.trim()
            if (cleanHost.isNotEmpty()) {
                val hostOnly = cleanHost
                    .replace("https://", "")
                    .replace("http://", "")

                allowHost(hostOnly, schemes = listOf("http", "https"))

                if (hostOnly.startsWith("localhost")) {
                    allowHost(hostOnly, schemes = listOf("http", "https"))
                }
            }
        }

       // anyHost()

        maxAgeInSeconds = 3600
    }
}