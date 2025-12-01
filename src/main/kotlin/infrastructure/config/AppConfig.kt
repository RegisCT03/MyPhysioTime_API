package com.myPhysioTime.infrastructure.config

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val server: ServerConfig
)

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationTime: Long
)

data class ServerConfig(
    val port: Int,
    val allowedHosts: List<String>
)