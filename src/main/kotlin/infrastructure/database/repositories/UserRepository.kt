package com.myPhysioTime.infrastructure.database.repositories

import com.myPhysioTime.domain.models.*
import com.myPhysioTime.domain.ports.UserRepository
import com.myPhysioTime.infrastructure.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.transactions.TransactionManager

class UserRepositoryImpl : UserRepository {

    override suspend fun findById(id: Int): User? = DatabaseFactory.dbQuery {
        (UsersTable innerJoin RolesTable)
            .select { UsersTable.id eq id }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    override suspend fun findByEmail(email: String): User? = DatabaseFactory.dbQuery {
        (UsersTable innerJoin RolesTable)
            .select { UsersTable.email eq email }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    override suspend fun findCredentialsByEmail(email: String): UserCredentials? = DatabaseFactory.dbQuery {
        (UsersTable innerJoin RolesTable)
            .select { UsersTable.email eq email }
            .mapNotNull { it.toUserCredentials() }
            .singleOrNull()
    }

    override suspend fun findAll(): List<User> = DatabaseFactory.dbQuery {
        (UsersTable innerJoin RolesTable)
            .selectAll()
            .mapNotNull { it.toUser() }
    }

    override suspend fun findAllClients(): List<ClientStats> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_client_stats ORDER BY name
        """.trimIndent()

        TransactionManager.current().exec(query) { rs ->
            val result = mutableListOf<ClientStats>()
            while (rs.next()) {
                result.add(
                    ClientStats(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        email = rs.getString("email"),
                        phone = rs.getString("phone"),
                        totalBookings = rs.getInt("total_bookings"),
                        lastVisit = rs.getTimestamp("last_visit")?.toLocalDateTime(),
                        preferredService = rs.getString("preferred_service")
                    )
                )
            }
            result
        } ?: emptyList()
    }

    override suspend fun create(command: CreateUserCommand): User = DatabaseFactory.dbQuery {
        val now = LocalDateTime.now()

        require(command.roleId in 1..2) { "Invalid roleId: must be 1 (ADMIN) or 2 (CLIENT)" }
        require(command.firstName.isNotBlank()) { "firstName cannot be blank" }
        require(command.lastName.isNotBlank()) { "lastName cannot be blank" }
        require(command.email.isNotBlank()) { "email cannot be blank" }
        require(command.password.isNotBlank()) { "password cannot be blank" }

        val phoneValue = command.phone?.take(10) ?: ""

        val insertedId = try {
            UsersTable.insert {
                it[roleId] = command.roleId
                it[firstName] = command.firstName
                it[lastName] = command.lastName
                it[email] = command.email
                it[phone] = phoneValue
                it[password] = command.password
                it[stripeId] = null
                it[lastLogin] = null
                it[createdAt] = now
                it[updatedAt] = now
            } get UsersTable.id
        } catch (e: Exception) {
            println("Error al insertar usuario: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()

            val message = when {
                e.message?.contains("duplicate key", ignoreCase = true) == true ->
                    "Email already exists"
                e.message?.contains("foreign key", ignoreCase = true) == true ->
                    "Invalid roleId"
                e.message?.contains("not-null constraint", ignoreCase = true) == true ->
                    "Missing required field"
                else -> "Database error: ${e.message}"
            }
            throw IllegalStateException(message)
        }

        println("Usuario insertado con ID: ${insertedId.value}")
        return@dbQuery try {
            Thread.sleep(50)

            val user = (UsersTable innerJoin RolesTable)
                .select { UsersTable.id eq insertedId.value }
                .map { it.toUser() }
                .singleOrNull()

            if (user == null) {
                println("⚠️ JOIN falló, intentando recuperar directamente...")

                val userRow = UsersTable.select { UsersTable.id eq insertedId.value }
                    .singleOrNull()

                if (userRow != null) {
                    val roleRow = RolesTable.select { RolesTable.id eq userRow[UsersTable.roleId] }
                        .singleOrNull()

                    if (roleRow != null) {
                        User(
                            id = userRow[UsersTable.id].value,
                            roleId = userRow[UsersTable.roleId].value,
                            roleName = RoleName.valueOf(roleRow[RolesTable.name].uppercase()),
                            stripeId = userRow[UsersTable.stripeId],
                            firstName = userRow[UsersTable.firstName],
                            lastName = userRow[UsersTable.lastName],
                            email = userRow[UsersTable.email],
                            phone = userRow[UsersTable.phone],
                            createdAt = userRow[UsersTable.createdAt],
                            lastLogin = userRow[UsersTable.lastLogin]
                        )
                    } else {
                        throw IllegalStateException("Role not found for roleId=${userRow[UsersTable.roleId].value}")
                    }
                } else {
                    throw IllegalStateException("User not found after insert with id=${insertedId.value}")
                }
            } else {
                user
            }
        } catch (e: Exception) {
            println("Error al recuperar usuario: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("Failed to retrieve created user with id=${insertedId.value}: ${e.message}")
        }
    }

    override suspend fun update(id: Int, command: UpdateUserCommand): User? = DatabaseFactory.dbQuery {
        val updated = UsersTable.update({ UsersTable.id eq id }) {
            command.firstName?.let { firstName -> it[UsersTable.firstName] = firstName }
            command.lastName?.let { lastName -> it[UsersTable.lastName] = lastName }
            command.phone?.let { phone -> it[UsersTable.phone] = phone.take(10) }
            it[updatedAt] = LocalDateTime.now()
        }

        if (updated > 0) findById(id) else null
    }

    override suspend fun delete(id: Int): Boolean = DatabaseFactory.dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    override suspend fun updateLastLogin(id: Int): Boolean = DatabaseFactory.dbQuery {
        UsersTable.update({ UsersTable.id eq id }) {
            it[lastLogin] = LocalDateTime.now()
        } > 0
    }
}

private fun ResultRow.toUser() = User(
    id = this[UsersTable.id].value,
    roleId = this[UsersTable.roleId].value,
    roleName = RoleName.valueOf(this[RolesTable.name].uppercase()),
    stripeId = this[UsersTable.stripeId],
    firstName = this[UsersTable.firstName],
    lastName = this[UsersTable.lastName],
    email = this[UsersTable.email],
    phone = this[UsersTable.phone],
    createdAt = this[UsersTable.createdAt],
    lastLogin = this[UsersTable.lastLogin]
)

private fun ResultRow.toUserCredentials() = UserCredentials(
    id = this[UsersTable.id].value,
    email = this[UsersTable.email],
    password = this[UsersTable.password],
    roleId = this[UsersTable.roleId].value,
    roleName = RoleName.valueOf(this[RolesTable.name].uppercase())
)