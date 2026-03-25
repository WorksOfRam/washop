package com.washop.repository

import com.washop.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class OrderRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("Asia/Kolkata"))

    fun create(
        shopId: String,
        customerPhone: String,
        customerName: String?,
        deliveryAddress: String?,
        items: List<OrderItemRequest>,
        products: Map<String, Product>,
        notes: String?,
        paymentMethod: String?
    ): Order = transaction {
        val orderId = "ORD_${UUID.randomUUID().toString().take(8).uppercase()}"

        var subtotal = 0.0
        items.forEach { item ->
            val product = products[item.productId]
            if (product != null) {
                subtotal += product.price * item.quantity
            }
        }

        val total = subtotal

        Orders.insert {
            it[id] = orderId
            it[Orders.shopId] = shopId
            it[Orders.customerPhone] = customerPhone
            it[Orders.customerName] = customerName
            it[Orders.deliveryAddress] = deliveryAddress
            it[Orders.subtotal] = subtotal.toBigDecimal()
            it[Orders.total] = total.toBigDecimal()
            it[Orders.notes] = notes
            it[Orders.paymentMethod] = paymentMethod
        }

        items.forEach { item ->
            val product = products[item.productId]
            if (product != null) {
                OrderItems.insert {
                    it[OrderItems.orderId] = orderId
                    it[productId] = item.productId
                    it[productName] = product.name
                    it[quantity] = item.quantity
                    it[unitPrice] = product.price.toBigDecimal()
                    it[totalPrice] = (product.price * item.quantity).toBigDecimal()
                }
            }
        }

        findById(orderId)!!
    }

    fun findById(orderId: String): Order? = transaction {
        val orderRow = Orders.selectAll()
            .where { Orders.id eq orderId }
            .singleOrNull() ?: return@transaction null

        val items = OrderItems.selectAll()
            .where { OrderItems.orderId eq orderId }
            .map { it.toOrderItemResponse() }

        orderRow.toOrder(items)
    }

    fun findByShopId(
        shopId: String,
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): OrderListResponse = transaction {
        val baseQuery = Orders.selectAll()
            .where { Orders.shopId eq shopId }

        if (status != null) {
            baseQuery.andWhere { Orders.status eq status }
        }

        val total = baseQuery.count().toInt()

        val orders = baseQuery
            .orderBy(Orders.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { orderRow ->
                val items = OrderItems.selectAll()
                    .where { OrderItems.orderId eq orderRow[Orders.id] }
                    .map { it.toOrderItemResponse() }
                orderRow.toOrder(items)
            }

        OrderListResponse(
            orders = orders,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    fun findByCustomerPhone(customerPhone: String, shopId: String? = null): List<Order> = transaction {
        val query = Orders.selectAll()
            .where { Orders.customerPhone eq customerPhone }

        if (shopId != null) {
            query.andWhere { Orders.shopId eq shopId }
        }

        query.orderBy(Orders.createdAt to SortOrder.DESC)
            .map { orderRow ->
                val items = OrderItems.selectAll()
                    .where { OrderItems.orderId eq orderRow[Orders.id] }
                    .map { it.toOrderItemResponse() }
                orderRow.toOrder(items)
            }
    }

    fun updateStatus(orderId: String, status: String): Order? = transaction {
        Orders.update({ Orders.id eq orderId }) {
            it[Orders.status] = status
            when (status) {
                "ACCEPTED" -> it[acceptedAt] = Instant.now()
                "DELIVERED" -> it[deliveredAt] = Instant.now()
            }
        }

        findById(orderId)
    }

    fun updatePaymentStatus(orderId: String, paymentStatus: String): Order? = transaction {
        Orders.update({ Orders.id eq orderId }) {
            it[Orders.paymentStatus] = paymentStatus
        }

        findById(orderId)
    }

    fun getOrderStats(shopId: String): OrderStats = transaction {
        val today = Instant.now().atZone(ZoneId.of("Asia/Kolkata"))
            .toLocalDate().atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()

        val totalOrders = Orders.selectAll()
            .where { Orders.shopId eq shopId }
            .count().toInt()

        val todayOrders = Orders.selectAll()
            .where { (Orders.shopId eq shopId) and (Orders.createdAt greaterEq today) }
            .count().toInt()

        val pendingOrders = Orders.selectAll()
            .where { (Orders.shopId eq shopId) and (Orders.status eq "PENDING") }
            .count().toInt()

        val totalRevenue = Orders.select(Orders.total.sum())
            .where { (Orders.shopId eq shopId) and (Orders.paymentStatus eq "PAID") }
            .map { it[Orders.total.sum()]?.toDouble() ?: 0.0 }
            .firstOrNull() ?: 0.0

        OrderStats(
            totalOrders = totalOrders,
            todayOrders = todayOrders,
            pendingOrders = pendingOrders,
            totalRevenue = totalRevenue
        )
    }

    private fun ResultRow.toOrder(items: List<OrderItemResponse>) = Order(
        id = this[Orders.id],
        shopId = this[Orders.shopId],
        customerPhone = this[Orders.customerPhone],
        customerName = this[Orders.customerName],
        deliveryAddress = this[Orders.deliveryAddress],
        subtotal = this[Orders.subtotal].toDouble(),
        deliveryCharge = this[Orders.deliveryCharge].toDouble(),
        discount = this[Orders.discount].toDouble(),
        total = this[Orders.total].toDouble(),
        status = this[Orders.status],
        paymentStatus = this[Orders.paymentStatus],
        paymentMethod = this[Orders.paymentMethod],
        notes = this[Orders.notes],
        items = items,
        createdAt = dateFormatter.format(this[Orders.createdAt])
    )

    private fun ResultRow.toOrderItemResponse() = OrderItemResponse(
        id = this[OrderItems.id],
        productId = this[OrderItems.productId],
        productName = this[OrderItems.productName],
        quantity = this[OrderItems.quantity],
        unitPrice = this[OrderItems.unitPrice].toDouble(),
        totalPrice = this[OrderItems.totalPrice].toDouble()
    )
}

@kotlinx.serialization.Serializable
data class OrderStats(
    val totalOrders: Int,
    val todayOrders: Int,
    val pendingOrders: Int,
    val totalRevenue: Double
)
