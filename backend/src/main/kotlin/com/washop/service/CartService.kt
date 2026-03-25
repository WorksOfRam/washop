package com.washop.service

import com.washop.models.*
import com.washop.repository.CartRepository
import com.washop.repository.ProductRepository

class CartService(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository
) {

    fun addToCart(request: AddToCartRequest): CartItem {
        val product = productRepository.findById(request.productId)
            ?: throw IllegalArgumentException("Product not found")

        if (!product.isAvailable) {
            throw IllegalArgumentException("Product is not available")
        }

        if (product.shopId != request.shopId) {
            throw IllegalArgumentException("Product does not belong to this shop")
        }

        return cartRepository.addItem(request)
    }

    fun getCart(userPhone: String, shopId: String): Cart {
        val items = cartRepository.getCart(userPhone, shopId)
        val subtotal = items.sumOf { it.itemTotal ?: 0.0 }

        return Cart(
            userPhone = userPhone,
            shopId = shopId,
            items = items,
            subtotal = subtotal,
            itemCount = items.sumOf { it.quantity }
        )
    }

    fun updateQuantity(userPhone: String, shopId: String, productId: String, quantity: Int): Boolean {
        return cartRepository.updateQuantity(userPhone, shopId, productId, quantity)
    }

    fun removeFromCart(request: RemoveFromCartRequest): Boolean {
        return cartRepository.removeItem(request.userPhone, request.shopId, request.productId)
    }

    fun clearCart(userPhone: String, shopId: String): Int {
        return cartRepository.clearCart(userPhone, shopId)
    }

    fun getCartItemCount(userPhone: String, shopId: String): Int {
        val items = cartRepository.getCart(userPhone, shopId)
        return items.sumOf { it.quantity }
    }
}
