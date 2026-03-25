package com.washop.repository

import com.washop.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class UserRepository {

    fun create(request: CreateUserRequest): User = transaction {
        Users.insert {
            it[phone] = request.phone
            it[name] = request.name
            it[role] = request.role
            it[shopId] = request.shopId
            it[defaultAddress] = request.defaultAddress
        }

        findByPhone(request.phone)!!
    }

    fun findByPhone(phone: String): User? = transaction {
        Users.selectAll()
            .where { Users.phone eq phone }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findOrCreate(phone: String, name: String? = null): User = transaction {
        findByPhone(phone) ?: create(CreateUserRequest(phone = phone, name = name))
    }

    fun findByShopId(shopId: String): List<User> = transaction {
        Users.selectAll()
            .where { Users.shopId eq shopId }
            .map { it.toUser() }
    }

    fun update(phone: String, request: UpdateUserRequest): User? = transaction {
        Users.update({ Users.phone eq phone }) {
            request.name?.let { name -> it[Users.name] = name }
            request.defaultAddress?.let { address -> it[defaultAddress] = address }
        }

        findByPhone(phone)
    }

    fun updateLastActive(phone: String) = transaction {
        Users.update({ Users.phone eq phone }) {
            it[lastActiveAt] = Instant.now()
        }
    }

    fun delete(phone: String): Boolean = transaction {
        Users.update({ Users.phone eq phone }) {
            it[isActive] = false
        } > 0
    }

    private fun ResultRow.toUser() = User(
        phone = this[Users.phone],
        name = this[Users.name],
        role = this[Users.role],
        shopId = this[Users.shopId],
        defaultAddress = this[Users.defaultAddress],
        isActive = this[Users.isActive]
    )
}
