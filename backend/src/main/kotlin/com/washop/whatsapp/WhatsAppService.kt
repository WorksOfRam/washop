package com.washop.whatsapp

import com.washop.config.WhatsAppConfig
import com.washop.models.*
import com.washop.service.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class WhatsAppService(
    private val whatsAppConfig: WhatsAppConfig,
    private val messageParser: MessageParser,
    private val productService: ProductService,
    private val cartService: CartService,
    private val orderService: OrderService,
    private val paymentService: PaymentService
) {
    private val logger = LoggerFactory.getLogger(WhatsAppService::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val defaultShopId = "SHOP_001"

    suspend fun handleMessage(from: String, messageText: String): String {
        logger.info("Received message from $from: $messageText")

        val command = messageParser.parse(messageText)
        val response = processCommand(from, command)

        sendMessage(from, response)
        return response
    }

    private suspend fun processCommand(userPhone: String, command: BotCommand): String {
        return when (command.type) {
            CommandType.GREETING -> getGreetingMessage()
            CommandType.LIST_PRODUCTS -> getProductListMessage()
            CommandType.ADD_TO_CART -> addToCartMessage(userPhone, command.productId, command.quantity ?: 1)
            CommandType.VIEW_CART -> getCartMessage(userPhone)
            CommandType.REMOVE_FROM_CART -> removeFromCartMessage(userPhone, command.productId)
            CommandType.CLEAR_CART -> clearCartMessage(userPhone)
            CommandType.CHECKOUT -> checkoutMessage(userPhone)
            CommandType.PAYMENT -> getPaymentMessage(userPhone)
            CommandType.ORDER_HISTORY -> getOrderHistoryMessage(userPhone)
            CommandType.ORDER_STATUS -> getOrderStatusMessage(command.orderId)
            CommandType.HELP -> getHelpMessage()
            CommandType.SEARCH -> searchProductsMessage(command.query)
            CommandType.SET_ADDRESS -> "Address saved: ${command.address}"
            CommandType.UNKNOWN -> getUnknownCommandMessage()
        }
    }

    private fun getGreetingMessage(): String {
        return """
            |🙏 *Welcome to WA Shop!*
            |
            |I'm here to help you order products.
            |
            |📋 *Quick Commands:*
            |• *products* - View all items
            |• *cart* - View your cart
            |• *checkout* - Place order
            |• *help* - All commands
            |
            |_Type "products" to start shopping!_
        """.trimMargin()
    }

    private fun getProductListMessage(): String {
        val products = productService.getProductsByShop(defaultShopId, availableOnly = true)

        if (products.isEmpty()) {
            return "No products available at the moment."
        }

        val productList = products.mapIndexed { index, product ->
            val num = index + 1
            "*$num.* ${product.name} - ₹${product.price.toInt()}/${product.unit}"
        }.joinToString("\n")

        return """
            |🛍️ *Available Products*
            |
            |$productList
            |
            |━━━━━━━━━━━━━━━━
            |_To add: Type the number_
            |_Example: "1" adds Rice_
            |_Or: "1 2" adds 2 Rice_
        """.trimMargin()
    }

    private fun addToCartMessage(userPhone: String, productId: String?, quantity: Int): String {
        if (productId.isNullOrBlank()) {
            return "Please specify a product. Type *products* to see the list."
        }

        val products = productService.getProductsByShop(defaultShopId, availableOnly = true)
        val productIndex = productId.toIntOrNull()

        val product = if (productIndex != null && productIndex > 0 && productIndex <= products.size) {
            products[productIndex - 1]
        } else {
            products.find { it.id == productId || it.name.lowercase().contains(productId.lowercase()) }
        }

        if (product == null) {
            return "Product not found. Type *products* to see available items."
        }

        return try {
            cartService.addToCart(
                AddToCartRequest(
                    userPhone = userPhone,
                    shopId = defaultShopId,
                    productId = product.id,
                    quantity = quantity
                )
            )

            val cart = cartService.getCart(userPhone, defaultShopId)

            """
                |✅ *Added to cart!*
                |
                |${product.name} x $quantity = ₹${(product.price * quantity).toInt()}
                |
                |🛒 Cart: ${cart.itemCount} items | Total: ₹${cart.subtotal.toInt()}
                |
                |_Type "cart" to view or "checkout" to order_
            """.trimMargin()
        } catch (e: Exception) {
            "Failed to add to cart: ${e.message}"
        }
    }

    private fun getCartMessage(userPhone: String): String {
        val cart = cartService.getCart(userPhone, defaultShopId)

        if (cart.items.isEmpty()) {
            return """
                |🛒 *Your cart is empty*
                |
                |_Type "products" to start shopping_
            """.trimMargin()
        }

        val itemList = cart.items.mapIndexed { index, item ->
            "${index + 1}. ${item.productName} x ${item.quantity} = ₹${item.itemTotal?.toInt()}"
        }.joinToString("\n")

        return """
            |🛒 *Your Cart*
            |
            |$itemList
            |
            |━━━━━━━━━━━━━━━━
            |*Total: ₹${cart.subtotal.toInt()}*
            |
            |• *checkout* - Place order
            |• *clear* - Empty cart
            |• *remove 1* - Remove item 1
        """.trimMargin()
    }

    private fun removeFromCartMessage(userPhone: String, productId: String?): String {
        if (productId.isNullOrBlank()) {
            return "Please specify which item to remove. Example: *remove 1*"
        }

        val cart = cartService.getCart(userPhone, defaultShopId)
        val itemIndex = productId.toIntOrNull()

        val item = if (itemIndex != null && itemIndex > 0 && itemIndex <= cart.items.size) {
            cart.items[itemIndex - 1]
        } else {
            null
        }

        if (item == null) {
            return "Item not found in cart."
        }

        cartService.removeFromCart(
            RemoveFromCartRequest(userPhone, defaultShopId, item.productId)
        )

        return "✅ Removed ${item.productName} from cart."
    }

    private fun clearCartMessage(userPhone: String): String {
        cartService.clearCart(userPhone, defaultShopId)
        return "🗑️ Cart cleared!"
    }

    private fun checkoutMessage(userPhone: String): String {
        val cart = cartService.getCart(userPhone, defaultShopId)

        if (cart.items.isEmpty()) {
            return "Your cart is empty. Add some products first!"
        }

        return try {
            val order = orderService.createOrderFromCart(
                userPhone = userPhone,
                shopId = defaultShopId,
                customerName = null,
                deliveryAddress = null,
                notes = "Order via WhatsApp",
                paymentMethod = "COD"
            )

            """
                |✅ *Order Placed Successfully!*
                |
                |📦 Order ID: *${order.id}*
                |💰 Total: *₹${order.total.toInt()}*
                |📍 Status: ${order.status}
                |
                |━━━━━━━━━━━━━━━━
                |The shop owner has been notified.
                |
                |• *pay* - Get payment link
                |• *order ${order.id}* - Check status
            """.trimMargin()
        } catch (e: Exception) {
            "Failed to place order: ${e.message}"
        }
    }

    private fun getPaymentMessage(userPhone: String): String {
        val orders = orderService.getOrdersByCustomer(userPhone, defaultShopId)
        val pendingOrder = orders.find { it.paymentStatus == "UNPAID" }

        if (pendingOrder == null) {
            return "No pending orders found."
        }

        return """
            |💳 *Payment for Order ${pendingOrder.id}*
            |
            |Amount: ₹${pendingOrder.total.toInt()}
            |
            |🔗 Payment options:
            |• UPI: yourshop@upi
            |• Cash on Delivery
            |
            |_Contact shop for other payment methods_
        """.trimMargin()
    }

    private fun getOrderHistoryMessage(userPhone: String): String {
        val orders = orderService.getOrdersByCustomer(userPhone, defaultShopId)

        if (orders.isEmpty()) {
            return "You haven't placed any orders yet."
        }

        val orderList = orders.take(5).map { order ->
            "• ${order.id} | ₹${order.total.toInt()} | ${order.status}"
        }.joinToString("\n")

        return """
            |📦 *Your Recent Orders*
            |
            |$orderList
            |
            |_Type "order <ID>" to see details_
        """.trimMargin()
    }

    private fun getOrderStatusMessage(orderId: String?): String {
        if (orderId.isNullOrBlank()) {
            return "Please provide an order ID. Example: *order ORD_12345678*"
        }

        val order = orderService.getOrder(orderId)
            ?: return "Order not found."

        val itemList = order.items.joinToString("\n") { item ->
            "• ${item.productName} x ${item.quantity}"
        }

        return """
            |📦 *Order Details*
            |
            |ID: ${order.id}
            |Status: ${order.status}
            |Payment: ${order.paymentStatus}
            |
            |*Items:*
            |$itemList
            |
            |*Total: ₹${order.total.toInt()}*
        """.trimMargin()
    }

    private fun getHelpMessage(): String {
        return """
            |📖 *Available Commands*
            |
            |🛍️ *Shopping*
            |• *products* - View items
            |• *1* or *add 1* - Add item 1
            |• *1 2* - Add 2 of item 1
            |• *search rice* - Search products
            |
            |🛒 *Cart*
            |• *cart* - View cart
            |• *remove 1* - Remove item
            |• *clear* - Empty cart
            |
            |📦 *Orders*
            |• *checkout* - Place order
            |• *pay* - Payment link
            |• *orders* - Order history
            |• *order ID* - Order status
            |
            |_Type "hi" to start!_
        """.trimMargin()
    }

    private fun searchProductsMessage(query: String?): String {
        if (query.isNullOrBlank()) {
            return "Please provide a search term. Example: *search rice*"
        }

        val products = productService.searchProducts(defaultShopId, query)

        if (products.isEmpty()) {
            return "No products found for '$query'"
        }

        val productList = products.map { product ->
            "• ${product.name} - ₹${product.price.toInt()}"
        }.joinToString("\n")

        return """
            |🔍 *Search Results for "$query"*
            |
            |$productList
            |
            |_Type "add <name>" to add to cart_
        """.trimMargin()
    }

    private fun getUnknownCommandMessage(): String {
        return """
            |Sorry, I didn't understand that.
            |
            |_Type "help" for available commands_
            |_Or "products" to see our catalog_
        """.trimMargin()
    }

    private suspend fun sendMessage(to: String, message: String) {
        if (whatsAppConfig.token.isBlank()) {
            logger.info("WhatsApp token not configured. Message to $to: $message")
            return
        }

        try {
            client.post("${whatsAppConfig.apiUrl}/${whatsAppConfig.phoneNumberId}/messages") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${whatsAppConfig.token}")
                setBody(
                    mapOf(
                        "messaging_product" to "whatsapp",
                        "to" to to,
                        "type" to "text",
                        "text" to mapOf("body" to message)
                    )
                )
            }
            logger.info("Message sent to $to")
        } catch (e: Exception) {
            logger.error("Failed to send message to $to: ${e.message}")
        }
    }
}
