package com.washop.routes

import com.washop.config.JwtConfig
import com.washop.models.*
import com.washop.service.AuthService
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authService by inject<AuthService>()
    val jwtConfig by inject<JwtConfig>()

    route("/auth") {
        post("/send-otp") {
            val request = call.receive<LoginRequest>()

            if (request.phone.length != 10) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid phone number"))
                return@post
            }

            val response = authService.sendOtp(request.phone)
            call.respond(HttpStatusCode.OK, ApiResponse.success(response))
        }

        post("/verify-otp") {
            val request = call.receive<VerifyOtpRequest>()

            val response = authService.verifyOtp(request.phone, request.otp, jwtConfig)
            if (response != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(response))
            } else {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Invalid or expired OTP"))
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            if (request.phone.length != 10) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid phone number"))
                return@post
            }

            val response = authService.loginWithoutOtp(request.phone, jwtConfig)
            call.respond(HttpStatusCode.OK, ApiResponse.success(response))
        }

        post("/register") {
            val request = call.receive<RegisterShopRequest>()

            if (request.phone.length != 10) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid phone number"))
                return@post
            }

            if (request.shopName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Shop name is required"))
                return@post
            }

            try {
                val response = authService.registerShopOwner(
                    phone = request.phone,
                    shopName = request.shopName,
                    ownerName = request.ownerName,
                    address = request.address,
                    jwtConfig = jwtConfig
                )
                call.respond(HttpStatusCode.Created, ApiResponse.success(response))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Registration failed"))
            }
        }
    }
}

@Serializable
data class RegisterShopRequest(
    val phone: String,
    val shopName: String,
    val ownerName: String? = null,
    val address: String? = null
)
