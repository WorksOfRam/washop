package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : Table("users") {
    val phone = varchar("phone", 15)
    val name = varchar("name", 255).nullable()
    val role = varchar("role", 20).default("CUSTOMER")
    val shopId = varchar("shop_id", 50).references(Shops.id).nullable()
    val defaultAddress = text("default_address").nullable()
    val isActive = bool("is_active").default(true)
    val lastActiveAt = timestamp("last_active_at").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(phone)
}

enum class UserRole {
    CUSTOMER,
    SHOP_OWNER,
    ADMIN
}

@Serializable
data class User(
    val phone: String,
    val name: String? = null,
    val role: String = "CUSTOMER",
    val shopId: String? = null,
    val defaultAddress: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class CreateUserRequest(
    val phone: String,
    val name: String? = null,
    val role: String = "CUSTOMER",
    val shopId: String? = null,
    val defaultAddress: String? = null
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val defaultAddress: String? = null
)
