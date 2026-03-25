package com.washop.routes

import com.washop.models.*
import com.washop.service.ProductService
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.productRoutes() {
    val productService by inject<ProductService>()

    route("/products") {
        get {
            val shopId = call.request.queryParameters["shopId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("shopId required"))

            val category = call.request.queryParameters["category"]
            val query = call.request.queryParameters["query"]
            val availableOnly = call.request.queryParameters["available"]?.toBoolean() ?: true

            val products = when {
                query != null -> productService.searchProducts(shopId, query)
                category != null -> productService.getProductsByCategory(shopId, category)
                else -> productService.getProductsByShop(shopId, availableOnly)
            }

            call.respond(HttpStatusCode.OK, ApiResponse.success(products))
        }

        get("/categories") {
            val shopId = call.request.queryParameters["shopId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("shopId required"))

            val categories = productService.getCategories(shopId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(categories))
        }

        get("/{id}") {
            val productId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Product ID required"))

            val product = productService.getProduct(productId)
            if (product != null) {
                call.respond(HttpStatusCode.OK, ApiResponse.success(product))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
            }
        }

        authenticate("auth-jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                val request = call.receive<CreateProductRequest>()

                if (userShopId != request.shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized to add products to this shop"))
                    return@post
                }

                try {
                    val product = productService.createProduct(request)
                    call.respond(HttpStatusCode.Created, ApiResponse.success(product))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to create product"))
                }
            }

            put("/{id}") {
                val productId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Product ID required"))

                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                val existingProduct = productService.getProduct(productId)
                if (existingProduct == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
                    return@put
                }

                if (userShopId != existingProduct.shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized to update this product"))
                    return@put
                }

                val request = call.receive<UpdateProductRequest>()

                try {
                    val product = productService.updateProduct(productId, request)
                    if (product != null) {
                        call.respond(HttpStatusCode.OK, ApiResponse.success(product))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Failed to update product"))
                }
            }

            put("/{id}/toggle") {
                val productId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Product ID required"))

                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                val existingProduct = productService.getProduct(productId)
                if (existingProduct == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
                    return@put
                }

                if (userShopId != existingProduct.shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized"))
                    return@put
                }

                val product = productService.toggleAvailability(productId)
                if (product != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(product))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
                }
            }

            delete("/{id}") {
                val productId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Product ID required"))

                val principal = call.principal<JWTPrincipal>()
                val userShopId = principal?.payload?.getClaim("shopId")?.asString()

                val existingProduct = productService.getProduct(productId)
                if (existingProduct == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
                    return@delete
                }

                if (userShopId != existingProduct.shopId) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse.error("Not authorized to delete this product"))
                    return@delete
                }

                val deleted = productService.deleteProduct(productId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("message" to "Product deleted")))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Product not found"))
                }
            }
        }
    }
}
