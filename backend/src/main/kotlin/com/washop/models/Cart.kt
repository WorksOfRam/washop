package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object CartItems : Table("cart_items") {
    val id = integer("id").autoIncrement()
    val userPhone = varchar("user_phone", 15)
    val shopId = varchar("shop_id", 50).references(Shops.id)
    val productId = varchar("product_id", 50).references(Products.id)
    val quantity = integer("quantity").default(1)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class CartItem(
    val id: Int,
    val userPhone: String,
    val shopId: String,
    val productId: String,
    val productName: String? = null,
    val productPrice: Double? = null,
    val quantity: Int,
    val itemTotal: Double? = null
)

@Serializable
data class Cart(
    val userPhone: String,
    val shopId: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val itemCount: Int
)

@Serializable
data class AddToCartRequest(
    val userPhone: String,
    val shopId: String,
    val productId: String,
    val quantity: Int = 1
)

@Serializable
data class UpdateCartRequest(
    val quantity: Int
)

@Serializable
data class RemoveFromCartRequest(
    val userPhone: String,
    val shopId: String,
    val productId: String
)
