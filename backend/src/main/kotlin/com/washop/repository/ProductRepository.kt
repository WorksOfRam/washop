package com.washop.repository

import com.washop.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ProductRepository {

    fun create(request: CreateProductRequest): Product = transaction {
        val productId = "P_${UUID.randomUUID().toString().take(8).uppercase()}"

        Products.insert {
            it[id] = productId
            it[shopId] = request.shopId
            it[name] = request.name
            it[description] = request.description
            it[price] = request.price.toBigDecimal()
            it[unit] = request.unit
            it[imageUrl] = request.imageUrl
            it[category] = request.category
            it[stock] = request.stock
        }

        findById(productId)!!
    }

    fun findById(productId: String): Product? = transaction {
        Products.selectAll()
            .where { Products.id eq productId }
            .map { it.toProduct() }
            .singleOrNull()
    }

    fun findByShopId(shopId: String, availableOnly: Boolean = false): List<Product> = transaction {
        val query = Products.selectAll()
            .where { Products.shopId eq shopId }

        if (availableOnly) {
            query.andWhere { Products.isAvailable eq true }
        }

        query.orderBy(Products.displayOrder to SortOrder.ASC, Products.name to SortOrder.ASC)
            .map { it.toProduct() }
    }

    fun findByCategory(shopId: String, category: String): List<Product> = transaction {
        Products.selectAll()
            .where { (Products.shopId eq shopId) and (Products.category eq category) and (Products.isAvailable eq true) }
            .orderBy(Products.displayOrder to SortOrder.ASC)
            .map { it.toProduct() }
    }

    fun searchByName(shopId: String, query: String): List<Product> = transaction {
        Products.selectAll()
            .where { (Products.shopId eq shopId) and (Products.name.lowerCase() like "%${query.lowercase()}%") }
            .map { it.toProduct() }
    }

    fun update(productId: String, request: UpdateProductRequest): Product? = transaction {
        Products.update({ Products.id eq productId }) {
            request.name?.let { name -> it[Products.name] = name }
            request.description?.let { desc -> it[description] = desc }
            request.price?.let { p -> it[price] = p.toBigDecimal() }
            request.unit?.let { u -> it[unit] = u }
            request.imageUrl?.let { url -> it[imageUrl] = url }
            request.category?.let { cat -> it[category] = cat }
            request.stock?.let { s -> it[stock] = s }
            request.isAvailable?.let { avail -> it[isAvailable] = avail }
            request.displayOrder?.let { order -> it[displayOrder] = order }
        }

        findById(productId)
    }

    fun updateStock(productId: String, quantity: Int): Boolean = transaction {
        val product = findById(productId) ?: return@transaction false
        if (product.stock == -1) return@transaction true

        Products.update({ Products.id eq productId }) {
            it[stock] = product.stock - quantity
        } > 0
    }

    fun delete(productId: String): Boolean = transaction {
        Products.deleteWhere { Products.id eq productId } > 0
    }

    fun getCategories(shopId: String): List<String> = transaction {
        Products.select(Products.category)
            .where { (Products.shopId eq shopId) and (Products.category.isNotNull()) }
            .withDistinct()
            .mapNotNull { it[Products.category] }
    }

    private fun ResultRow.toProduct() = Product(
        id = this[Products.id],
        shopId = this[Products.shopId],
        name = this[Products.name],
        description = this[Products.description],
        price = this[Products.price].toDouble(),
        unit = this[Products.unit],
        imageUrl = this[Products.imageUrl],
        category = this[Products.category],
        stock = this[Products.stock],
        isAvailable = this[Products.isAvailable],
        displayOrder = this[Products.displayOrder]
    )
}
