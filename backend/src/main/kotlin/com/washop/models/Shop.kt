package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Shops : Table("shops") {
    val id = varchar("id", 50)
    val name = varchar("name", 255)
    val ownerPhone = varchar("owner_phone", 15)
    val ownerName = varchar("owner_name", 255).nullable()
    val address = text("address").nullable()
    val city = varchar("city", 100).default("Vizag")
    val whatsappNumber = varchar("whatsapp_number", 15).nullable()
    val isActive = bool("is_active").default(true)
    val subscriptionPlan = varchar("subscription_plan", 50).default("FREE")
    val subscriptionExpiresAt = timestamp("subscription_expires_at").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class Shop(
    val id: String,
    val name: String,
    val ownerPhone: String,
    val ownerName: String? = null,
    val address: String? = null,
    val city: String = "Vizag",
    val whatsappNumber: String? = null,
    val isActive: Boolean = true,
    val subscriptionPlan: String = "FREE"
)

@Serializable
data class CreateShopRequest(
    val name: String,
    val ownerPhone: String,
    val ownerName: String? = null,
    val address: String? = null,
    val city: String = "Vizag"
)

@Serializable
data class UpdateShopRequest(
    val name: String? = null,
    val ownerName: String? = null,
    val address: String? = null,
    val whatsappNumber: String? = null,
    val isActive: Boolean? = null
)
