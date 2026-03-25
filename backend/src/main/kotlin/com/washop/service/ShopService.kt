package com.washop.service

import com.washop.models.*
import com.washop.repository.ShopRepository

class ShopService(
    private val shopRepository: ShopRepository
) {

    fun createShop(request: CreateShopRequest): Shop {
        val existing = shopRepository.findByOwnerPhone(request.ownerPhone)
        if (existing != null) {
            throw IllegalArgumentException("Shop already exists for this phone number")
        }
        return shopRepository.create(request)
    }

    fun getShop(shopId: String): Shop? {
        return shopRepository.findById(shopId)
    }

    fun getShopByOwnerPhone(phone: String): Shop? {
        return shopRepository.findByOwnerPhone(phone)
    }

    fun getAllShops(): List<Shop> {
        return shopRepository.findAll()
    }

    fun updateShop(shopId: String, request: UpdateShopRequest): Shop? {
        return shopRepository.update(shopId, request)
    }

    fun deleteShop(shopId: String): Boolean {
        return shopRepository.delete(shopId)
    }
}
