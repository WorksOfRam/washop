package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Otps : Table("otps") {
    val id = integer("id").autoIncrement()
    val phone = varchar("phone", 15)
    val otp = varchar("otp", 6)
    val purpose = varchar("purpose", 20).default("LOGIN")
    val isUsed = bool("is_used").default(false)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

object Sessions : Table("sessions") {
    val id = integer("id").autoIncrement()
    val userPhone = varchar("user_phone", 15).references(Users.phone)
    val tokenHash = varchar("token_hash", 255)
    val deviceInfo = text("device_info").nullable()
    val isActive = bool("is_active").default(true)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class LoginRequest(
    val phone: String
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: User,
    val shop: Shop? = null
)

@Serializable
data class OtpResponse(
    val phone: String,
    val message: String,
    val expiresInSeconds: Int = 300
)
