package com.washop.config

import io.ktor.server.application.*

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$name"
}

data class RedisConfig(
    val host: String,
    val port: Int
) {
    val url: String
        get() = "redis://$host:$port"
}

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiration: Long
)

data class WhatsAppConfig(
    val token: String,
    val verifyToken: String,
    val phoneNumberId: String,
    val apiUrl: String
)

data class RazorpayConfig(
    val keyId: String,
    val keySecret: String
)

data class AppConfig(
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val jwt: JwtConfig,
    val whatsApp: WhatsAppConfig,
    val razorpay: RazorpayConfig
)

fun Application.loadConfig(): AppConfig {
    val config = environment.config

    return AppConfig(
        database = DatabaseConfig(
            host = config.property("database.host").getString(),
            port = config.property("database.port").getString().toInt(),
            name = config.property("database.name").getString(),
            user = config.property("database.user").getString(),
            password = config.property("database.password").getString()
        ),
        redis = RedisConfig(
            host = config.property("redis.host").getString(),
            port = config.property("redis.port").getString().toInt()
        ),
        jwt = JwtConfig(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            realm = config.property("jwt.realm").getString(),
            expiration = config.property("jwt.expiration").getString().toLong()
        ),
        whatsApp = WhatsAppConfig(
            token = config.property("whatsapp.token").getString(),
            verifyToken = config.property("whatsapp.verifyToken").getString(),
            phoneNumberId = config.property("whatsapp.phoneNumberId").getString(),
            apiUrl = config.property("whatsapp.apiUrl").getString()
        ),
        razorpay = RazorpayConfig(
            keyId = config.property("razorpay.keyId").getString(),
            keySecret = config.property("razorpay.keySecret").getString()
        )
    )
}
