package com.washop.config

import com.washop.repository.*
import com.washop.service.*
import com.washop.whatsapp.MessageParser
import com.washop.whatsapp.WhatsAppService
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val appConfig = loadConfig()

    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single { appConfig }
                single { appConfig.jwt }
                single { appConfig.whatsApp }
                single { appConfig.razorpay }
            },
            repositoryModule,
            serviceModule,
            whatsAppModule
        )
    }
}

val repositoryModule = module {
    single { ShopRepository() }
    single { UserRepository() }
    single { ProductRepository() }
    single { CartRepository() }
    single { OrderRepository() }
    single { PaymentRepository() }
    single { OtpRepository() }
}

val serviceModule = module {
    single { AuthService(get(), get(), get()) }
    single { ShopService(get()) }
    single { ProductService(get()) }
    single { CartService(get(), get()) }
    single { OrderService(get(), get(), get()) }
    single { PaymentService(get(), get()) }
}

val whatsAppModule = module {
    single { MessageParser() }
    single { WhatsAppService(get(), get(), get(), get(), get(), get()) }
}
