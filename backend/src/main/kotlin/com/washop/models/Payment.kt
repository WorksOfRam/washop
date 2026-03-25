package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Payments : Table("payments") {
    val id = varchar("id", 50)
    val orderId = varchar("order_id", 50).references(Orders.id)
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 10).default("INR")
    val status = varchar("status", 20).default("PENDING")
    val gateway = varchar("gateway", 50).default("RAZORPAY")
    val gatewayPaymentId = varchar("gateway_payment_id", 255).nullable()
    val gatewayOrderId = varchar("gateway_order_id", 255).nullable()
    val paymentLink = text("payment_link").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

enum class PaymentGatewayStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}

@Serializable
data class Payment(
    val id: String,
    val orderId: String,
    val amount: Double,
    val currency: String = "INR",
    val status: String = "PENDING",
    val gateway: String = "RAZORPAY",
    val gatewayPaymentId: String? = null,
    val gatewayOrderId: String? = null,
    val paymentLink: String? = null
)

@Serializable
data class CreatePaymentRequest(
    val orderId: String
)

@Serializable
data class PaymentWebhookRequest(
    val orderId: String,
    val paymentId: String,
    val status: String,
    val signature: String? = null
)

@Serializable
data class PaymentLinkResponse(
    val paymentId: String,
    val orderId: String,
    val amount: Double,
    val paymentLink: String
)
