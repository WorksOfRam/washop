package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Orders : Table("orders") {
    val id = varchar("id", 50)
    val shopId = varchar("shop_id", 50).references(Shops.id)
    val customerPhone = varchar("customer_phone", 15)
    val customerName = varchar("customer_name", 255).nullable()
    val deliveryAddress = text("delivery_address").nullable()
    val subtotal = decimal("subtotal", 10, 2).default(java.math.BigDecimal.ZERO)
    val deliveryCharge = decimal("delivery_charge", 10, 2).default(java.math.BigDecimal.ZERO)
    val discount = decimal("discount", 10, 2).default(java.math.BigDecimal.ZERO)
    val total = decimal("total", 10, 2).default(java.math.BigDecimal.ZERO)
    val status = varchar("status", 20).default("PENDING")
    val paymentStatus = varchar("payment_status", 20).default("UNPAID")
    val paymentMethod = varchar("payment_method", 20).nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
    val acceptedAt = timestamp("accepted_at").nullable()
    val deliveredAt = timestamp("delivered_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val id = integer("id").autoIncrement()
    val orderId = varchar("order_id", 50).references(Orders.id)
    val productId = varchar("product_id", 50).references(Products.id).nullable()
    val productName = varchar("product_name", 255)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2)
    val totalPrice = decimal("total_price", 10, 2)
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

enum class OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

enum class PaymentStatus {
    UNPAID,
    PAID,
    REFUNDED,
    COD
}

@Serializable
data class Order(
    val id: String,
    val shopId: String,
    val customerPhone: String,
    val customerName: String? = null,
    val deliveryAddress: String? = null,
    val subtotal: Double,
    val deliveryCharge: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double,
    val status: String = "PENDING",
    val paymentStatus: String = "UNPAID",
    val paymentMethod: String? = null,
    val notes: String? = null,
    val items: List<OrderItemResponse> = emptyList(),
    val createdAt: String? = null
)

@Serializable
data class OrderItemResponse(
    val id: Int,
    val productId: String?,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

@Serializable
data class CreateOrderRequest(
    val shopId: String,
    val customerPhone: String,
    val customerName: String? = null,
    val deliveryAddress: String? = null,
    val items: List<OrderItemRequest>,
    val notes: String? = null,
    val paymentMethod: String? = null
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val quantity: Int
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: String
)

@Serializable
data class OrderListResponse(
    val orders: List<Order>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)
