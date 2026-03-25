package com.washop.repository

import com.washop.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CartRepository {

    fun addItem(request: AddToCartRequest): CartItem = transaction {
        val existing = findItem(request.userPhone, request.shopId, request.productId)

        if (existing != null) {
            CartItems.update({
                (CartItems.userPhone eq request.userPhone) and
                (CartItems.shopId eq request.shopId) and
                (CartItems.productId eq request.productId)
            }) {
                it[quantity] = existing.quantity + request.quantity
            }
            findItem(request.userPhone, request.shopId, request.productId)!!
        } else {
            val id = CartItems.insert {
                it[userPhone] = request.userPhone
                it[shopId] = request.shopId
                it[productId] = request.productId
                it[quantity] = request.quantity
            } get CartItems.id

            CartItem(
                id = id,
                userPhone = request.userPhone,
                shopId = request.shopId,
                productId = request.productId,
                quantity = request.quantity
            )
        }
    }

    fun findItem(userPhone: String, shopId: String, productId: String): CartItem? = transaction {
        CartItems.selectAll()
            .where {
                (CartItems.userPhone eq userPhone) and
                (CartItems.shopId eq shopId) and
                (CartItems.productId eq productId)
            }
            .map { it.toCartItem() }
            .singleOrNull()
    }

    fun getCart(userPhone: String, shopId: String): List<CartItem> = transaction {
        (CartItems innerJoin Products)
            .selectAll()
            .where {
                (CartItems.userPhone eq userPhone) and
                (CartItems.shopId eq shopId)
            }
            .map {
                CartItem(
                    id = it[CartItems.id],
                    userPhone = it[CartItems.userPhone],
                    shopId = it[CartItems.shopId],
                    productId = it[CartItems.productId],
                    productName = it[Products.name],
                    productPrice = it[Products.price].toDouble(),
                    quantity = it[CartItems.quantity],
                    itemTotal = it[Products.price].toDouble() * it[CartItems.quantity]
                )
            }
    }

    fun updateQuantity(userPhone: String, shopId: String, productId: String, quantity: Int): Boolean = transaction {
        if (quantity <= 0) {
            removeItem(userPhone, shopId, productId)
        } else {
            CartItems.update({
                (CartItems.userPhone eq userPhone) and
                (CartItems.shopId eq shopId) and
                (CartItems.productId eq productId)
            }) {
                it[CartItems.quantity] = quantity
            } > 0
        }
    }

    fun removeItem(userPhone: String, shopId: String, productId: String): Boolean = transaction {
        CartItems.deleteWhere {
            (CartItems.userPhone eq userPhone) and
            (CartItems.shopId eq shopId) and
            (CartItems.productId eq productId)
        } > 0
    }

    fun clearCart(userPhone: String, shopId: String): Int = transaction {
        CartItems.deleteWhere {
            (CartItems.userPhone eq userPhone) and
            (CartItems.shopId eq shopId)
        }
    }

    fun clearUserCart(userPhone: String): Int = transaction {
        CartItems.deleteWhere {
            CartItems.userPhone eq userPhone
        }
    }

    private fun ResultRow.toCartItem() = CartItem(
        id = this[CartItems.id],
        userPhone = this[CartItems.userPhone],
        shopId = this[CartItems.shopId],
        productId = this[CartItems.productId],
        quantity = this[CartItems.quantity]
    )
}
