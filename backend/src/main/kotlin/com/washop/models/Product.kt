package com.washop.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Products : Table("products") {
    val id = varchar("id", 50)
    val shopId = varchar("shop_id", 50).references(Shops.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val price = decimal("price", 10, 2)
    val unit = varchar("unit", 50).default("piece")
    val imageUrl = text("image_url").nullable()
    val category = varchar("category", 100).nullable()
    val stock = integer("stock").default(-1)
    val isAvailable = bool("is_available").default(true)
    val displayOrder = integer("display_order").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class Product(
    val id: String,
    val shopId: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val unit: String = "piece",
    val imageUrl: String? = null,
    val category: String? = null,
    val stock: Int = -1,
    val isAvailable: Boolean = true,
    val displayOrder: Int = 0
)

@Serializable
data class CreateProductRequest(
    val shopId: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val unit: String = "piece",
    val imageUrl: String? = null,
    val category: String? = null,
    val stock: Int = -1
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val unit: String? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    val stock: Int? = null,
    val isAvailable: Boolean? = null,
    val displayOrder: Int? = null
)
