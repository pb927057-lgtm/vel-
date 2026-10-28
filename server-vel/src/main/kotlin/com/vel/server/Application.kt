package com.vel.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8443, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = 65536
        masking = false
    }

    val revocationRegistry = RevocationRegistry()
    val relayEngine = RelayEngine(revocationRegistry)

    routing {
        webSocket("/v1/relay") {
            val deviceId = call.request.queryParameters["deviceId"]
            val role = call.request.queryParameters["role"]
            val authToken = call.request.queryParameters["auth"]

            if (deviceId.isNullOrBlank() || role.isNullOrBlank() || authToken.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Faltan credenciales de conexión"))
                return@webSocket
            }

            // Si el terminal ya está revocado, denegar conexión inmediatamente
            if (revocationRegistry.isRevoked(deviceId)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Dispositivo revocado por CORE"))
                return@webSocket
            }

            relayEngine.registerSession(deviceId, this)

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()

                        // Manejar instrucción de revocación enviada desde CORE
                        if (role == "CORE" && text.contains("REVOCATION_INSTRUCTION")) {
                            // Extraer targetTerminalId y registrar en lista de revocados
                            val match = Regex("\"targetTerminalId\":\"([^\"]+)\"").find(text)
                            match?.groupValues?.get(1)?.let { revokedId ->
                                revocationRegistry.recordRevocation(revokedId)
                            }
                        } else {
                            // Retransmisión de bytes cifrados (Cero conocimiento)
                            relayEngine.routeEncryptedMessage(text)
                        }
                    }
                }
            } finally {
                relayEngine.removeSession(deviceId)
            }
        }
    }
}
