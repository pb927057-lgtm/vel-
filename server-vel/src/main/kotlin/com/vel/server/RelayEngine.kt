package com.vel.server

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class RelayPacket(
    val protocolVersion: Int,
    val requestId: String,
    val sourceDeviceId: String,
    val targetDeviceId: String,
    val nonceBase64: String,
    val ciphertextBase64: String,
    val aadBase64: String,
    val timestamp: Long
)

/**
 * Motor de Retransmisión de Cero Conocimiento (Zero-Knowledge Relay).
 * El servidor es estrictamente incapaz de descifrar (Parte VI.25):
 * No dispone de claves simétricas ni asimétricas, solo enruta el sobre cifrado.
 */
class RelayEngine(
    private val revocationRegistry: RevocationRegistry
) {
    // deviceId -> WebSocketSession
    private val activeSessions = ConcurrentHashMap<String, WebSocketSession>()

    fun registerSession(deviceId: String, session: WebSocketSession) {
        activeSessions[deviceId] = session
    }

    fun removeSession(deviceId: String) {
        activeSessions.remove(deviceId)
    }

    suspend fun routeEncryptedMessage(rawJson: String): Boolean {
        val packet = try {
            Json.decodeFromString<RelayPacket>(rawJson)
        } catch (e: Exception) {
            return false
        }

        // 1. Verificar si el remitente está revocado
        if (revocationRegistry.isRevoked(packet.sourceDeviceId)) {
            // Rechazar de inmediato cualquier retransmisión originada por un dispositivo revocado
            return false
        }

        // 2. Localizar la sesión de destino
        val targetSession = activeSessions[packet.targetDeviceId] ?: return false

        // 3. Reenviar los bytes cifrados intactos
        targetSession.send(Frame.Text(rawJson))
        return true
    }
}
