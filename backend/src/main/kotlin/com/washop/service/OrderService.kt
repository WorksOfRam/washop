package com.washop.service

import com.washop.models.*
import com.washop.repository.CartRepository
import com.washop.repository.OrderRepository
import com.washop.repository.ProductRepository
import com.washop.repository.OrderStats

class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) {

    fun createOrder(request: CreateOrderRequest): Order {
        if (request.items.isEmpty()) {
            throw IllegalArgumentException("Order must have at least one item")
        }

        val productIds = request.items.map { it.productId }
        val products = productIds.associateWith { productRepository.findById(it) }
            .filterValues { it != null }
            .mapValues { it.value!! }

        if (products.size != request.items.size) {
            throw IllegalArgumentException("One or more products not found")
        }

        products.values.forEach { product ->
            if (!product.isAvailable) {
                throw IllegalArgumentException("Product ${product.name} is not available")
            }
            if (product.shopId != request.shopId) {
                throw IllegalArgumentException("Product ${product.name} does not belong to this shop")
            }
        }

        val order = orderRepository.create(
            shopId = request.shopId,
            customerPhone = request.customerPhone,
            customerName = request.customerName,
            deliveryAddress = request.deliveryAddress,
            items = request.items,
            products = products,
            notes = request.notes,
            paymentMethod = request.paymentMethod
        )

        request.items.forEach { item ->
            productRepository.updateStock(item.productId, item.quantity)
        }

        cartRepository.clearCart(request.customerPhone, request.shopId)

        return order
    }

    fun createOrderFromCart(
        userPhone: String,
        shopId: String,
        customerName: String?,
        deliveryAddress: String?,
        notes: String?,
        paymentMethod: String?
    ): Order {
        val cartItems = cartRepository.getCart(userPhone, shopId)
        if (cartItems.isEmpty()) {
            throw IllegalArgumentException("Cart is empty")
        }

        val items = cartItems.map { OrderItemRequest(it.productId, it.quantity) }

        return createOrder(
            CreateOrderRequest(
                shopId = shopId,
                customerPhone = userPhone,
                customerName = customerName,
                deliveryAddress = deliveryAddress,
                items = items,
                notes = notes,
                paymentMethod = paymentMethod
            )
        )
    }

    fun getOrder(orderId: String): Order? {
        return orderRepository.findById(orderId)
    }

    fun getOrdersByShop(
        shopId: String,
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): OrderListResponse {
        return orderRepository.findByShopId(shopId, status, page, pageSize)
    }

    fun getOrdersByCustomer(customerPhone: String, shopId: String? = null): List<Order> {
        return orderRepository.findByCustomerPhone(customerPhone, shopId)
    }

    fun updateOrderStatus(orderId: String, status: String): Order? {
        val validStatuses = listOf("PENDING", "ACCEPTED", "REJECTED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")
        if (status !in validStatuses) {
            throw IllegalArgumentException("Invalid status: $status")
        }
        return orderRepository.updateStatus(orderId, status)
    }

    fun updatePaymentStatus(orderId: String, paymentStatus: String): Order? {
        val validStatuses = listOf("UNPAID", "PAID", "REFUNDED", "COD")
        if (paymentStatus !in validStatuses) {
            throw IllegalArgumentException("Invalid payment status: $paymentStatus")
        }
        return orderRepository.updatePaymentStatus(orderId, paymentStatus)
    }

    fun getOrderStats(shopId: String): OrderStats {
        return orderRepository.getOrderStats(shopId)
    }
}
