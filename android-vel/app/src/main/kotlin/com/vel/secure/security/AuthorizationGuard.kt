package com.vel.secure.security

import com.vel.secure.data.local.DeviceDao
import com.vel.secure.domain.model.AuthorizationStatus
import com.vel.secure.domain.model.ProtectedOperationRequest
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor de tokens individuales de autorización por Terminal (Parte VII).
 * Cada Terminal autorizado posee un token criptográficamente impredecible.
 */
class TokenManager {
    private val secureRandom = SecureRandom()

    fun generateIndividualToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Guardián de Autorización y Prevención de Repetición (Replay Attacks) (Parte VII.27).
 */
class AuthorizationGuard(
    private val deviceDao: DeviceDao,
    private val tokenManager: TokenManager
) {
    // Almacén de IDs de solicitud para prevenir ataques de repetición (Replay)
    private val processedRequestIds = ConcurrentHashMap<String, Long>()
    private val MAX_CLOCK_SKEW_MS = 60_000L // 60 segundos de tolerancia de reloj

    sealed class ValidationResult {
        object Allowed : ValidationResult()
        data class Denied(val reason: String) : ValidationResult()
    }

    suspend fun validateOperation(
        terminalId: String,
        request: ProtectedOperationRequest
    ): ValidationResult {
        // 1. Verificar si la solicitud ya fue procesada (Anti-Replay)
        if (processedRequestIds.containsKey(request.requestId)) {
            return ValidationResult.Denied("Ataque de repetición detectado: solicitud duplicada (${request.requestId})")
        }

        // 2. Verificar frescura temporal
        val now = System.currentTimeMillis()
        if (Math.abs(now - request.clientTimestamp) > MAX_CLOCK_SKEW_MS) {
            return ValidationResult.Denied("Ventana de tiempo expirada o reloj desfasado")
        }

        // 3. Consultar existencia del dispositivo en Room
        val device = deviceDao.getDeviceById(terminalId)
            ?: return ValidationResult.Denied("Dispositivo desconocido no registrado en CORE")

        // 4. Comprobar que no esté revocado
        if (device.authorizationStatus == AuthorizationStatus.REVOKED.name) {
            return ValidationResult.Denied("Acceso denegado: el dispositivo $terminalId ha sido REVOCADO")
        }

        if (device.authorizationStatus != AuthorizationStatus.AUTHORIZED.name) {
            return ValidationResult.Denied("Dispositivo en estado no autorizado (${device.authorizationStatus})")
        }

        // 5. Validar hash del token de autorización individual
        val providedHash = tokenManager.hashToken(request.authToken)
        if (providedHash != device.authTokenHash) {
            return ValidationResult.Denied("Token de autorización inválido o no correspondiente al dispositivo")
        }

        // Registrar requestId para evitar repetición
        processedRequestIds[request.requestId] = now
        cleanupOldRequests(now)

        return ValidationResult.Allowed
    }

    private fun cleanupOldRequests(currentTime: Long) {
        val cutoff = currentTime - (MAX_CLOCK_SKEW_MS * 2)
        processedRequestIds.entries.removeIf { it.value < cutoff }
    }
}
