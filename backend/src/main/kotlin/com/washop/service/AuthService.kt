package com.washop.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.washop.config.JwtConfig
import com.washop.models.*
import com.washop.repository.OtpRepository
import com.washop.repository.ShopRepository
import com.washop.repository.UserRepository
import java.util.Date
import kotlin.random.Random

class AuthService(
    private val userRepository: UserRepository,
    private val shopRepository: ShopRepository,
    private val otpRepository: OtpRepository
) {

    fun sendOtp(phone: String): OtpResponse {
        val otp = generateOtp()
        otpRepository.create(phone, otp)

        println("OTP for $phone: $otp")

        return OtpResponse(
            phone = phone,
            message = "OTP sent successfully",
            expiresInSeconds = 300
        )
    }

    fun verifyOtp(phone: String, otp: String, jwtConfig: JwtConfig): LoginResponse? {
        val isValid = otpRepository.verify(phone, otp)
        if (!isValid) return null

        val user = userRepository.findOrCreate(phone)
        userRepository.updateLastActive(phone)

        val shop = user.shopId?.let { shopRepository.findById(it) }
            ?: shopRepository.findByOwnerPhone(phone)

        val token = generateToken(user, jwtConfig)

        return LoginResponse(
            token = token,
            user = user,
            shop = shop
        )
    }

    fun loginWithoutOtp(phone: String, jwtConfig: JwtConfig): LoginResponse {
        val user = userRepository.findOrCreate(phone)
        userRepository.updateLastActive(phone)

        val shop = user.shopId?.let { shopRepository.findById(it) }
            ?: shopRepository.findByOwnerPhone(phone)

        val token = generateToken(user, jwtConfig)

        return LoginResponse(
            token = token,
            user = user,
            shop = shop
        )
    }

    fun registerShopOwner(
        phone: String,
        shopName: String,
        ownerName: String?,
        address: String?,
        jwtConfig: JwtConfig
    ): LoginResponse {
        val shop = shopRepository.create(
            CreateShopRequest(
                name = shopName,
                ownerPhone = phone,
                ownerName = ownerName,
                address = address
            )
        )

        val existingUser = userRepository.findByPhone(phone)
        val user = if (existingUser != null) {
            userRepository.update(phone, UpdateUserRequest(name = ownerName))
                ?: existingUser
        } else {
            userRepository.create(
                CreateUserRequest(
                    phone = phone,
                    name = ownerName,
                    role = "SHOP_OWNER",
                    shopId = shop.id
                )
            )
        }

        val token = generateToken(user, jwtConfig)

        return LoginResponse(
            token = token,
            user = user,
            shop = shop
        )
    }

    private fun generateOtp(): String {
        return (100000 + Random.nextInt(900000)).toString()
    }

    private fun generateToken(user: User, jwtConfig: JwtConfig): String {
        return JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("phone", user.phone)
            .withClaim("role", user.role)
            .withClaim("shopId", user.shopId)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expiration))
            .sign(Algorithm.HMAC256(jwtConfig.secret))
    }
}
