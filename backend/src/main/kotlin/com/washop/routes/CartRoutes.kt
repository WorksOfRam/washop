package com.washop.routes

import com.washop.models.*
import com.washop.service.CartService
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.cartRoutes() {
    val cartService by inject<CartService>()

    route("/cart") {
        get {
            val userPhone = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("userId required"))

            val shopId = call.request.queryParameters["shopId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("shopId required"))

            val cart = cartService.getCart(userPhone, shopId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(cart))
        }

        post("/add") {
            val request = call.receive<AddToCartRequest>()

            try {
                val item = cartService.addToCart(request)
                val cart = cartService.getCart(request.userPhone, request.shopId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(cart))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to add to cart"))
            }
        }

        put("/update") {
            val request = call.receive<UpdateCartItemRequest>()

            val success = cartService.updateQuantity(
                request.userPhone,
                request.shopId,
                request.productId,
                request.quantity
            )

            if (success) {
                val cart = cartService.getCart(request.userPhone, request.shopId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(cart))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error("Cart item not found"))
            }
        }

        post("/remove") {
            val request = call.receive<RemoveFromCartRequest>()

            val removed = cartService.removeFromCart(request)
            if (removed) {
                val cart = cartService.getCart(request.userPhone, request.shopId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(cart))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error("Cart item not found"))
            }
        }

        delete("/clear") {
            val userPhone = call.request.queryParameters["userId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error("userId required"))

            val shopId = call.request.queryParameters["shopId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error("shopId required"))

            val count = cartService.clearCart(userPhone, shopId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("itemsRemoved" to count)))
        }
    }
}

@Serializable
data class UpdateCartItemRequest(
    val userPhone: String,
    val shopId: String,
    val productId: String,
    val quantity: Int
)
