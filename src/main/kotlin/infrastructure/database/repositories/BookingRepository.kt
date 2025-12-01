package com.myPhysioTime.infrastructure.database.repositories

import com.myPhysioTime.domain.models.*
import com.myPhysioTime.domain.ports.BookingRepository
import com.myPhysioTime.infrastructure.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.sql.Connection

val conn: Connection = (TransactionManager.current().connection as JdbcConnectionImpl).connection

class BookingRepositoryImpl : BookingRepository {

    override suspend fun findById(id: Int): BookingDetail? = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_bookings_detail WHERE id = ?
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.toBookingDetail() else null
            }
        }
    }

    override suspend fun findAll(): List<BookingDetail> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_bookings_detail ORDER BY date DESC
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<BookingDetail>()
                while (rs.next()) {
                    result.add(rs.toBookingDetail())
                }
                result.toList()
            }
        }
    }

    override suspend fun findByClientId(clientId: Int): List<BookingDetail> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_bookings_detail 
            WHERE client_id = ? 
            ORDER BY date DESC
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.setInt(1, clientId)
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<BookingDetail>()
                while (rs.next()) {
                    result.add(rs.toBookingDetail())
                }
                result.toList()
            }
        }
    }

    override suspend fun findByDate(date: LocalDate): List<BookingDetail> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_bookings_detail 
            WHERE DATE(date) = ? 
            ORDER BY date
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.setDate(1, java.sql.Date.valueOf(date))
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<BookingDetail>()
                while (rs.next()) {
                    result.add(rs.toBookingDetail())
                }
                result.toList()
            }
        }
    }

    override suspend fun findByState(state: BookingState): List<BookingDetail> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_bookings_detail 
            WHERE state = ?::bookings_states 
            ORDER BY date DESC
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.setString(1, state.name.lowercase())
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<BookingDetail>()
                while (rs.next()) {
                    result.add(rs.toBookingDetail())
                }
                result.toList()
            }
        }
    }

    override suspend fun findByDateRange(from: LocalDateTime, to: LocalDateTime): List<BookingDetail> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM v_bookings_detail 
            WHERE date BETWEEN ? AND ? 
            ORDER BY date
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(from))
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(to))
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<BookingDetail>()
                while (rs.next()) {
                    result.add(rs.toBookingDetail())
                }
                result.toList()
            }
        }
    }

    override suspend fun create(command: CreateBookingCommand): Booking = DatabaseFactory.dbQuery {
        val now = LocalDateTime.now()
        val id = BookingsTable.insert {
            it[serviceId] = command.serviceId
            it[clientId] = command.clientId
            it[date] = command.date
            it[state] = BookingState.PENDING.name.lowercase()
            it[notes] = command.notes
            it[createdAt] = now
            it[updatedAt] = now
        } get BookingsTable.id

        BookingsTable
            .select { BookingsTable.id eq id }
            .map { it.toBooking() }
            .single()
    }

    override suspend fun update(id: Int, command: UpdateBookingCommand): Booking? = DatabaseFactory.dbQuery {
        val updated = BookingsTable.update({ BookingsTable.id eq id }) {
            command.state?.let { state -> it[BookingsTable.state] = state.name.lowercase() }
            command.notes?.let { notes -> it[BookingsTable.notes] = notes }
            command.physiotherapeutId?.let { physioId -> it[physiotherapeutId] = physioId }
            it[updatedAt] = LocalDateTime.now()
        }

        if (updated > 0) {
            BookingsTable
                .select { BookingsTable.id eq id }
                .map { it.toBooking() }
                .singleOrNull()
        } else null
    }

    override suspend fun delete(id: Int): Boolean = DatabaseFactory.dbQuery {
        BookingsTable.deleteWhere { BookingsTable.id eq id } > 0
    }

    override suspend fun getAvailableSlots(date: LocalDate, duration: Int): List<TimeSlot> = DatabaseFactory.dbQuery {
        val query = """
            SELECT * FROM get_available_slots(?, ?)
        """.trimIndent()

        val conn = (TransactionManager.current().connection as JdbcConnectionImpl).connection

        conn.prepareStatement(query).use { stmt ->
            stmt.setDate(1, java.sql.Date.valueOf(date))
            stmt.setInt(2, duration)
            stmt.executeQuery().use { rs ->
                val result = mutableListOf<TimeSlot>()
                while (rs.next()) {
                    result.add(
                        TimeSlot(
                            time = rs.getTime("slot_time").toString(),
                            isAvailable = rs.getBoolean("is_available")
                        )
                    )
                }
                result.toList()
            }
        }
    }

    private fun ResultRow.toBooking() = Booking(
        id = this[BookingsTable.id].value,
        serviceId = this[BookingsTable.serviceId].value,
        clientId = this[BookingsTable.clientId].value,
        physiotherapeutId = this[BookingsTable.physiotherapeutId]?.value,
        date = this[BookingsTable.date],
        state = BookingState.valueOf(this[BookingsTable.state].uppercase()),
        notes = this[BookingsTable.notes],
        createdAt = this[BookingsTable.createdAt]
    )

    private fun java.sql.ResultSet.toBookingDetail() = BookingDetail(
        id = getInt("id"),
        date = getTimestamp("date").toLocalDateTime(),
        state = BookingState.valueOf(getString("state").uppercase()),
        notes = getString("notes"),
        service = ServiceInfo(
            id = getInt("service_id"),
            name = getString("service_name"),
            price = getDouble("service_price"),
            duration = getInt("service_duration")
        ),
        client = ClientInfo(
            id = getInt("client_id"),
            name = getString("client_name"),
            email = getString("client_email"),
            phone = getString("client_phone")
        ),
        physiotherapeut = if (getInt("physiotherapeut_id") != 0) {
            PhysiotherapeutInfo(
                id = getInt("physiotherapeut_id"),
                name = getString("physiotherapeut_name")
            )
        } else null,
        createdAt = getTimestamp("created_at").toLocalDateTime()
    )
    override suspend fun getDashboardStats(): DashboardStats = DatabaseFactory.dbQuery {
        val today = LocalDate.now()

        val totalToday = BookingsTable
            .select {
                BookingsTable.date.castTo<String>(VarCharColumnType())
                    .like("${today}%")
            }
            .count()
            .toInt()

        val pending = BookingsTable
            .select { BookingsTable.state eq BookingState.PENDING.name.lowercase() }
            .count()
            .toInt()

        val completed = BookingsTable
            .select { BookingsTable.state eq BookingState.COMPLETED.name.lowercase() }
            .count()
            .toInt()

        val totalClients = (UsersTable innerJoin RolesTable)
            .select { RolesTable.name eq "client" }
            .count()
            .toInt()

        DashboardStats(
            totalBookingsToday = totalToday,
            pendingBookings = pending,
            completedBookings = completed,
            totalClients = totalClients
        )
    }
}