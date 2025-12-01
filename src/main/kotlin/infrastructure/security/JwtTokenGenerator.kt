package com.myPhysioTime.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.myPhysioTime.domain.models.RoleName
import com.myPhysioTime.domain.ports.TokenGenerator
import com.myPhysioTime.domain.ports.TokenPayload
import java.util.*

class JwtTokenGenerator(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expirationTime: Long
) : TokenGenerator {

    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    override fun generate(userId: Int, email: String, role: RoleName): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + expirationTime))
            .sign(algorithm)
    }

    override fun verify(token: String): TokenPayload? {
        return try {
            val jwt: DecodedJWT = JWT.require(algorithm)
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
                .verify(token)

            TokenPayload(
                userId = jwt.getClaim("userId").asInt(),
                email = jwt.getClaim("email").asString(),
                role = RoleName.valueOf(jwt.getClaim("role").asString())
            )
        } catch (e: Exception) {
            null
        }
    }
}