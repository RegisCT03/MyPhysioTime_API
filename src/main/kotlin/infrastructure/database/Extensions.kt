package com.myPhysioTime.infrastructure.database

import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.PreparedStatement
import java.sql.ResultSet

fun <T> Transaction.exec(
    sql: String,
    setParams: (PreparedStatement.() -> Unit)? = null,
    transform: (ResultSet) -> T
): T? {
    val jdbcConnection = (TransactionManager.current().connection as JdbcConnectionImpl).connection

    jdbcConnection.prepareStatement(sql).use { statement ->
        setParams?.invoke(statement)
        statement.executeQuery().use { rs ->
            return transform(rs)
        }
    }
}