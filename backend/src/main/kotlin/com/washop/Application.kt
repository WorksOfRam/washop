package com.washop

import com.washop.config.configureDatabase
import com.washop.config.configureKoin
import com.washop.config.configureRouting
import com.washop.config.configureSecurity
import com.washop.config.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureKoin()
    configureDatabase()
    configureSerialization()
    configureSecurity()
    configureRouting()
}
