package com.myPhysioTime.application.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS(allowedHosts: List<String>) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        // Configurar hosts permitidos desde .env
        allowedHosts.forEach { host ->
            allowHost(host, schemes = listOf("http", "https"))
        }
    }
}