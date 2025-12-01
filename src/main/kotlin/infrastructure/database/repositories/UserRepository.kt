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

        println("Insertando usuario con: $command")

        requireNotNull(command.roleId) { "roleId no puede ser null" }
        requireNotNull(command.firstName) { "firstName no puede ser null" }
        requireNotNull(command.lastName) { "lastName no puede ser null" }
        requireNotNull(command.email) { "email no puede ser null" }
        requireNotNull(command.password) { "password no puede ser null" }

        val insertedId = try {
            UsersTable.insert {
                it[roleId] = command.roleId
                it[firstName] = command.firstName
                it[lastName] = command.lastName
                it[email] = command.email
                it[phone] = command.phone ?: ""
                it[password] = command.password
                it[stripeId] = null // explícito
                it[lastLogin] = null // explícito
                it[createdAt] = now
                it[updatedAt] = now
            } get UsersTable.id
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException("Falló el insert: ${e.message}")
        }

        println("ID insertado: ${insertedId.value}")

        // Verificación directa en tabla
        val userRow = UsersTable.select { UsersTable.id eq insertedId.value }.singleOrNull()
        if (userRow == null) {
            println("El usuario con id=${insertedId.value} no existe en la tabla después del insert")
            throw IllegalStateException("El usuario con id=${insertedId.value} no fue insertado correctamente")
        }

        println("Usuario insertado en tabla: $userRow")

        // Recuperación completa con join
        return@dbQuery findById(insertedId.value)
            ?: throw IllegalStateException("No se pudo recuperar el usuario insertado con id=${insertedId.value}")
    }

    override suspend fun update(id: Int, command: UpdateUserCommand): User? = DatabaseFactory.dbQuery {
        val updated = UsersTable.update({ UsersTable.id eq id }) {
            command.firstName?.let { firstName -> it[UsersTable.firstName] = firstName }
            command.lastName?.let { lastName -> it[UsersTable.lastName] = lastName }
            command.phone?.let { phone -> it[UsersTable.phone] = phone }
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

// Extension functions para mapear ResultRow a modelos
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