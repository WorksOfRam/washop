package com.washop.service

import com.washop.models.*
import com.washop.repository.ProductRepository

class ProductService(
    private val productRepository: ProductRepository
) {

    fun createProduct(request: CreateProductRequest): Product {
        if (request.price < 0) {
            throw IllegalArgumentException("Price cannot be negative")
        }
        return productRepository.create(request)
    }

    fun getProduct(productId: String): Product? {
        return productRepository.findById(productId)
    }

    fun getProductsByShop(shopId: String, availableOnly: Boolean = false): List<Product> {
        return productRepository.findByShopId(shopId, availableOnly)
    }

    fun getProductsByCategory(shopId: String, category: String): List<Product> {
        return productRepository.findByCategory(shopId, category)
    }

    fun searchProducts(shopId: String, query: String): List<Product> {
        return productRepository.searchByName(shopId, query)
    }

    fun updateProduct(productId: String, request: UpdateProductRequest): Product? {
        if (request.price != null && request.price < 0) {
            throw IllegalArgumentException("Price cannot be negative")
        }
        return productRepository.update(productId, request)
    }

    fun deleteProduct(productId: String): Boolean {
        return productRepository.delete(productId)
    }

    fun getCategories(shopId: String): List<String> {
        return productRepository.getCategories(shopId)
    }

    fun toggleAvailability(productId: String): Product? {
        val product = productRepository.findById(productId) ?: return null
        return productRepository.update(
            productId,
            UpdateProductRequest(isAvailable = !product.isAvailable)
        )
    }
}
