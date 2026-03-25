package com.washop.repository

import com.washop.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PaymentRepository {

    fun create(orderId: String, amount: Double): Payment = transaction {
        val paymentId = "PAY_${UUID.randomUUID().toString().take(8).uppercase()}"

        Payments.insert {
            it[id] = paymentId
            it[Payments.orderId] = orderId
            it[Payments.amount] = amount.toBigDecimal()
        }

        findById(paymentId)!!
    }

    fun findById(paymentId: String): Payment? = transaction {
        Payments.selectAll()
            .where { Payments.id eq paymentId }
            .map { it.toPayment() }
            .singleOrNull()
    }

    fun findByOrderId(orderId: String): Payment? = transaction {
        Payments.selectAll()
            .where { Payments.orderId eq orderId }
            .orderBy(Payments.createdAt to SortOrder.DESC)
            .map { it.toPayment() }
            .firstOrNull()
    }

    fun findByGatewayPaymentId(gatewayPaymentId: String): Payment? = transaction {
        Payments.selectAll()
            .where { Payments.gatewayPaymentId eq gatewayPaymentId }
            .map { it.toPayment() }
            .singleOrNull()
    }

    fun updateStatus(paymentId: String, status: String, gatewayPaymentId: String? = null): Payment? = transaction {
        Payments.update({ Payments.id eq paymentId }) {
            it[Payments.status] = status
            gatewayPaymentId?.let { gid -> it[Payments.gatewayPaymentId] = gid }
        }

        findById(paymentId)
    }

    fun updatePaymentLink(paymentId: String, paymentLink: String, gatewayOrderId: String): Payment? = transaction {
        Payments.update({ Payments.id eq paymentId }) {
            it[Payments.paymentLink] = paymentLink
            it[Payments.gatewayOrderId] = gatewayOrderId
        }

        findById(paymentId)
    }

    private fun ResultRow.toPayment() = Payment(
        id = this[Payments.id],
        orderId = this[Payments.orderId],
        amount = this[Payments.amount].toDouble(),
        currency = this[Payments.currency],
        status = this[Payments.status],
        gateway = this[Payments.gateway],
        gatewayPaymentId = this[Payments.gatewayPaymentId],
        gatewayOrderId = this[Payments.gatewayOrderId],
        paymentLink = this[Payments.paymentLink]
    )
}
