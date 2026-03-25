package com.washop.routes

import com.washop.models.*
import com.washop.service.ShopService
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shopRoutes() {
    val shopService by inject<ShopService>()

    route("/shops") {
        get {
            val shops = shopService.getAllShops()
            call.respond(HttpStatusCode.OK, ApiResponse.success(shops))
        }

        get("/{id}") {
            val shopId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Shop ID required"))

            val shop = shopService.getShop(shopId)
            if (shop != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(shop))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error("Shop not found"))
            }
        }

        authenticate("auth-jwt") {
            post {
                val request = call.receive<CreateShopRequest>()

                try {
                    val shop = shopService.createShop(request)
                    call.respond(HttpStatusCode.Created, ApiResponse.success(shop))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to create shop"))
                }
            }

            put("/{id}") {
                val shopId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Shop ID required"))

                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                if (userShopId != shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized to update this shop"))
                    return@put
                }

                val request = call.receive<UpdateShopRequest>()
                val shop = shopService.updateShop(shopId, request)

                if (shop != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(shop))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Shop not found"))
                }
            }

            delete("/{id}") {
                val shopId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Shop ID required"))

                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                if (userShopId != shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized to delete this shop"))
                    return@delete
                }

                val deleted = shopService.deleteShop(shopId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Shop deleted")))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Shop not found"))
                }
            }
        }
    }
}
