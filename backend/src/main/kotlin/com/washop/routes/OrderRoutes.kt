package com.washop.routes

import com.washop.models.*
import com.washop.service.OrderService
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.orderRoutes() {
    val orderService by inject<OrderService>()

    route("/orders") {
        post {
            val request = call.receive<CreateOrderRequest>()

            try {
                val order = orderService.createOrder(request)
                call.respond(HttpStatusCode.Created, ApiResponse.success(order))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to create order"))
            }
        }

        post("/from-cart") {
            val request = call.receive<CreateOrderFromCartRequest>()

            try {
                val order = orderService.createOrderFromCart(
                    userPhone = request.userPhone,
                    shopId = request.shopId,
                    customerName = request.customerName,
                    deliveryAddress = request.deliveryAddress,
                    notes = request.notes,
                    paymentMethod = request.paymentMethod
                )
                call.respond(HttpStatusCode.Created, ApiResponse.success(order))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to create order"))
            }
        }

        get {
            val shopId = call.request.queryParameters["shopId"]
            val customerPhone = call.request.queryParameters["customerPhone"]
            val status = call.request.queryParameters["status"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            when {
                shopId != null -> {
                    val response = orderService.getOrdersByShop(shopId, status, page, pageSize)
                    call.respond(HttpStatusCode.OK, ApiResponse.success(response))
                }
                customerPhone != null -> {
                    val orders = orderService.getOrdersByCustomer(customerPhone)
                    call.respond(HttpStatusCode.OK, ApiResponse.success(orders))
                }
                else -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error("shopId or customerPhone required"))
                }
            }
        }

        get("/stats") {
            val shopId = call.request.queryParameters["shopId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("shopId required"))

            val stats = orderService.getOrderStats(shopId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(stats))
        }

        get("/{id}") {
            val orderId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Order ID required"))

            val order = orderService.getOrder(orderId)
            if (order != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(order))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error("Order not found"))
            }
        }

        authenticate("auth-jwt") {
            put("/{id}/status") {
                val orderId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Order ID required"))

                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                val existingOrder = orderService.getOrder(orderId)
                if (existingOrder == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Order not found"))
                    return@put
                }

                if (userShopId != existingOrder.shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized"))
                    return@put
                }

                val request = call.receive<UpdateOrderStatusRequest>()

                try {
                    val order = orderService.updateOrderStatus(orderId, request.status)
                    if (order != null) {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(order))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse.error("Order not found"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Invalid status"))
                }
            }
        }
    }
}

@Serializable
data class CreateOrderFromCartRequest(
    val userPhone: String,
    val shopId: String,
    val customerName: String? = null,
    val deliveryAddress: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null
)
