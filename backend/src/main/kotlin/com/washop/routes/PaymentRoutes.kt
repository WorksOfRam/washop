package com.washop.routes

import com.washop.config.RazorpayConfig
import com.washop.models.*
import com.washop.service.PaymentService
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.paymentRoutes() {
    val paymentService by inject<PaymentService>()
    val razorpayConfig by inject<RazorpayConfig>()

    route("/payments") {
        post("/create") {
            val request = call.receive<CreatePaymentRequest>()

            try {
                val response = paymentService.createPaymentLink(request.orderId, razorpayConfig)
                call.respond(HttpStatusCode.OK, ApiResponse.success(response))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to create payment"))
            }
        }

        post("/webhook") {
            val request = call.receive<PaymentWebhookRequest>()

            val payment = paymentService.handleWebhook(request)
            if (payment != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(payment))
            } else {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid webhook"))
            }
        }

        get("/{orderId}") {
            val orderId = call.parameters["orderId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Order ID required"))

            val payment = paymentService.getPaymentByOrderId(orderId)
            if (payment != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(payment))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error("Payment not found"))
            }
        }
    }
}
