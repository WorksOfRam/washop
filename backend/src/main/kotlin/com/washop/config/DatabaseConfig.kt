package com.washop.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseConfig")

fun Application.configureDatabase() {
    val appConfig = loadConfig()
    val dbConfig = appConfig.database

    logger.info("Connecting to database: ${dbConfig.jdbcUrl}")

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbConfig.jdbcUrl
        driverClassName = "org.postgresql.Driver"
        username = dbConfig.user
        password = dbConfig.password
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 30000
        connectionTimeout = 30000
        maxLifetime = 1800000
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    logger.info("Database connection established successfully")
}
