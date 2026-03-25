package com.washop.repository

import com.washop.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ShopRepository {

    fun create(request: CreateShopRequest): Shop = transaction {
        val shopId = "SHOP_${UUID.randomUUID().toString().take(8).uppercase()}"

        Shops.insert {
            it[id] = shopId
            it[name] = request.name
            it[ownerPhone] = request.ownerPhone
            it[ownerName] = request.ownerName
            it[address] = request.address
            it[city] = request.city
        }

        findById(shopId)!!
    }

    fun findById(shopId: String): Shop? = transaction {
        Shops.selectAll()
            .where { Shops.id eq shopId }
            .map { it.toShop() }
            .singleOrNull()
    }

    fun findByOwnerPhone(phone: String): Shop? = transaction {
        Shops.selectAll()
            .where { Shops.ownerPhone eq phone }
            .map { it.toShop() }
            .singleOrNull()
    }

    fun findAll(): List<Shop> = transaction {
        Shops.selectAll()
            .where { Shops.isActive eq true }
            .map { it.toShop() }
    }

    fun update(shopId: String, request: UpdateShopRequest): Shop? = transaction {
        Shops.update({ Shops.id eq shopId }) {
            request.name?.let { name -> it[Shops.name] = name }
            request.ownerName?.let { ownerName -> it[Shops.ownerName] = ownerName }
            request.address?.let { address -> it[Shops.address] = address }
            request.whatsappNumber?.let { whatsappNumber -> it[Shops.whatsappNumber] = whatsappNumber }
            request.isActive?.let { isActive -> it[Shops.isActive] = isActive }
        }

        findById(shopId)
    }

    fun delete(shopId: String): Boolean = transaction {
        Shops.update({ Shops.id eq shopId }) {
            it[isActive] = false
        } > 0
    }

    private fun ResultRow.toShop() = Shop(
        id = this[Shops.id],
        name = this[Shops.name],
        ownerPhone = this[Shops.ownerPhone],
        ownerName = this[Shops.ownerName],
        address = this[Shops.address],
        city = this[Shops.city],
        whatsappNumber = this[Shops.whatsappNumber],
        isActive = this[Shops.isActive],
        subscriptionPlan = this[Shops.subscriptionPlan]
    )
}
