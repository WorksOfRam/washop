package com.washop.whatsapp

import com.washop.config.WhatsAppConfig
import com.washop.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("WhatsAppRoutes")

fun Route.whatsAppRoutes() {
    val whatsAppService by inject<WhatsAppService>()
    val whatsAppConfig by inject<WhatsAppConfig>()

    route("/webhook") {
        get {
            val mode = call.request.queryParameters["hub.mode"]
            val token = call.request.queryParameters["hub.verify_token"]
            val challenge = call.request.queryParameters["hub.challenge"]

            logger.info("Webhook verification: mode=$mode, token=$token")

            if (mode == "subscribe" && token == whatsAppConfig.verifyToken) {
                logger.info("Webhook verified successfully")
                call.respondText(challenge ?: "", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                logger.warn("Webhook verification failed")
                call.respond(HttpStatusCode.Forbidden, "Verification failed")
            }
        }

        post {
            try {
                val payload = call.receive<WebhookPayload>()
                logger.info("Received webhook: ${payload.entry?.size} entries")

                payload.entry?.forEach { entry ->
                    entry.changes?.forEach { change ->
                        val messages = change.value?.messages
                        messages?.forEach { message ->
                            val from = message.from
                            val text = message.text?.body

                            if (from != null && text != null) {
                                logger.info("Processing message from $from: $text")
                                whatsAppService.handleMessage(from, text)
                            }
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, ApiResponse.success("OK"))
            } catch (e: Exception) {
                logger.error("Error processing webhook: ${e.message}", e)
                call.respond(HttpStatusCode.OK, ApiResponse.success("OK"))
            }
        }

        post("/test") {
            val request = call.receive<TestMessageRequest>()

            val response = whatsAppService.handleMessage(request.from, request.message)
            call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("response" to response)))
        }
    }
}

@Serializable
data class WebhookPayload(
    val entry: List<WebhookEntry>? = null
)

@Serializable
data class WebhookEntry(
    val id: String? = null,
    val changes: List<WebhookChange>? = null
)

@Serializable
data class WebhookChange(
    val value: WebhookValue? = null,
    val field: String? = null
)

@Serializable
data class WebhookValue(
    val messaging_product: String? = null,
    val messages: List<WebhookMessage>? = null,
    val contacts: List<WebhookContact>? = null
)

@Serializable
data class WebhookMessage(
    val from: String? = null,
    val id: String? = null,
    val timestamp: String? = null,
    val type: String? = null,
    val text: WebhookText? = null
)

@Serializable
data class WebhookText(
    val body: String? = null
)

@Serializable
data class WebhookContact(
    val profile: WebhookProfile? = null,
    val wa_id: String? = null
)

@Serializable
data class WebhookProfile(
    val name: String? = null
)

@Serializable
data class TestMessageRequest(
    val from: String,
    val message: String
)
