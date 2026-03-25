package com.washop.service

import com.washop.config.RazorpayConfig
import com.washop.models.*
import com.washop.repository.OrderRepository
import com.washop.repository.PaymentRepository

class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) {

    fun createPaymentLink(orderId: String, razorpayConfig: RazorpayConfig): PaymentLinkResponse {
        val order = orderRepository.findById(orderId)
            ?: throw IllegalArgumentException("Order not found")

        if (order.paymentStatus == "PAID") {
            throw IllegalArgumentException("Order is already paid")
        }

        val existingPayment = paymentRepository.findByOrderId(orderId)
        if (existingPayment != null && existingPayment.status == "PENDING" && existingPayment.paymentLink != null) {
            return PaymentLinkResponse(
                paymentId = existingPayment.id,
                orderId = orderId,
                amount = existingPayment.amount,
                paymentLink = existingPayment.paymentLink!!
            )
        }

        val payment = paymentRepository.create(orderId, order.total)

        val paymentLink = generatePaymentLink(payment, order, razorpayConfig)

        paymentRepository.updatePaymentLink(payment.id, paymentLink, "RZP_${payment.id}")

        return PaymentLinkResponse(
            paymentId = payment.id,
            orderId = orderId,
            amount = order.total,
            paymentLink = paymentLink
        )
    }

    fun handleWebhook(request: PaymentWebhookRequest): Payment? {
        val payment = paymentRepository.findByOrderId(request.orderId)
            ?: return null

        val newStatus = when (request.status.uppercase()) {
            "PAID", "SUCCESS", "CAPTURED" -> "SUCCESS"
            "FAILED" -> "FAILED"
            else -> return null
        }

        paymentRepository.updateStatus(payment.id, newStatus, request.paymentId)

        if (newStatus == "SUCCESS") {
            orderRepository.updatePaymentStatus(request.orderId, "PAID")
        }

        return paymentRepository.findById(payment.id)
    }

    fun getPaymentByOrderId(orderId: String): Payment? {
        return paymentRepository.findByOrderId(orderId)
    }

    private fun generatePaymentLink(payment: Payment, order: Order, config: RazorpayConfig): String {
        if (config.keyId.isBlank()) {
            return "https://payment.example.com/pay/${payment.id}?amount=${order.total}"
        }

        return "https://rzp.io/i/${payment.id}"
    }
}
