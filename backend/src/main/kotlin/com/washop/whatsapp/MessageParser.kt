package com.washop.whatsapp

import kotlinx.serialization.Serializable

class MessageParser {

    fun parse(message: String): BotCommand {
        val normalizedMessage = message.trim().lowercase()

        return when {
            normalizedMessage in listOf("hi", "hello", "hey", "start", "menu") -> {
                BotCommand(CommandType.GREETING)
            }

            normalizedMessage in listOf("products", "list", "catalog", "items", "show products") -> {
                BotCommand(CommandType.LIST_PRODUCTS)
            }

            normalizedMessage.startsWith("add ") || normalizedMessage.startsWith("add_") -> {
                val parts = normalizedMessage.removePrefix("add").removePrefix("_").trim().split(" ")
                val productId = parts.getOrNull(0) ?: ""
                val quantity = parts.getOrNull(1)?.toIntOrNull() ?: 1
                BotCommand(CommandType.ADD_TO_CART, productId = productId, quantity = quantity)
            }

            normalizedMessage.matches(Regex("^\\d+$")) -> {
                BotCommand(CommandType.ADD_TO_CART, productId = normalizedMessage, quantity = 1)
            }

            normalizedMessage.matches(Regex("^\\d+\\s+\\d+$")) -> {
                val parts = normalizedMessage.split(" ")
                BotCommand(
                    CommandType.ADD_TO_CART,
                    productId = parts[0],
                    quantity = parts[1].toIntOrNull() ?: 1
                )
            }

            normalizedMessage in listOf("cart", "view cart", "my cart", "show cart") -> {
                BotCommand(CommandType.VIEW_CART)
            }

            normalizedMessage.startsWith("remove ") -> {
                val productId = normalizedMessage.removePrefix("remove").trim()
                BotCommand(CommandType.REMOVE_FROM_CART, productId = productId)
            }

            normalizedMessage in listOf("clear", "clear cart", "empty cart") -> {
                BotCommand(CommandType.CLEAR_CART)
            }

            normalizedMessage in listOf("checkout", "order", "place order", "buy", "confirm") -> {
                BotCommand(CommandType.CHECKOUT)
            }

            normalizedMessage in listOf("pay", "payment", "pay now") -> {
                BotCommand(CommandType.PAYMENT)
            }

            normalizedMessage in listOf("orders", "my orders", "order history", "history") -> {
                BotCommand(CommandType.ORDER_HISTORY)
            }

            normalizedMessage.startsWith("order ") || normalizedMessage.startsWith("status ") -> {
                val orderId = normalizedMessage.removePrefix("order").removePrefix("status").trim()
                BotCommand(CommandType.ORDER_STATUS, orderId = orderId)
            }

            normalizedMessage in listOf("help", "?", "commands") -> {
                BotCommand(CommandType.HELP)
            }

            normalizedMessage.startsWith("search ") -> {
                val query = normalizedMessage.removePrefix("search").trim()
                BotCommand(CommandType.SEARCH, query = query)
            }

            normalizedMessage.startsWith("address ") -> {
                val address = message.removePrefix("address").removePrefix("Address").trim()
                BotCommand(CommandType.SET_ADDRESS, address = address)
            }

            else -> {
                BotCommand(CommandType.UNKNOWN, rawMessage = message)
            }
        }
    }
}

enum class CommandType {
    GREETING,
    LIST_PRODUCTS,
    ADD_TO_CART,
    VIEW_CART,
    REMOVE_FROM_CART,
    CLEAR_CART,
    CHECKOUT,
    PAYMENT,
    ORDER_HISTORY,
    ORDER_STATUS,
    HELP,
    SEARCH,
    SET_ADDRESS,
    UNKNOWN
}

@Serializable
data class BotCommand(
    val type: CommandType,
    val productId: String? = null,
    val quantity: Int? = null,
    val orderId: String? = null,
    val query: String? = null,
    val address: String? = null,
    val rawMessage: String? = null
)
