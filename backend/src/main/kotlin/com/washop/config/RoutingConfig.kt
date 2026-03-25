package com.washop.config

import com.washop.routes.*
import com.washop.whatsapp.whatsAppRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "success" to true,
                    "message" to "WA Shop API v1.0",
                    "endpoints" to listOf(
                        "/api/auth",
                        "/api/shops",
                        "/api/products",
                        "/api/cart",
                        "/api/orders",
                        "/api/payments",
                        "/webhook"
                    )
                )
            )
        }

        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "success" to true,
                    "status" to "healthy"
                )
            )
        }

        route("/api") {
            authRoutes()
            shopRoutes()
            productRoutes()
            cartRoutes()
            orderRoutes()
            paymentRoutes()
        }

        whatsAppRoutes()
    }
}
