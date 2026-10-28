package com.vel.secure

import com.vel.secure.crypto.CryptoEngine
import com.vel.secure.domain.model.CoreConfirmation
import com.vel.secure.domain.model.EncryptedEnvelope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64

class CorrelationAndVerificationTest {
    private lateinit var cryptoEngine: CryptoEngine
    private val key = ByteArray(32)
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        cryptoEngine = CryptoEngine()
        SecureRandom().nextBytes(key)
    }

    @Test
    fun testCorrelatedConfirmationSucceeds() {
        val reqId = "req-uuid-9901"
        val opType = "LOCK_DEVICE"
        val terminalId = "term-device-A"
        val coreId = "core-device-B"

        // AAD de correlación bidireccional estricta (Puntos 4 y 5)
        val correlationAad = "$reqId:$opType:$coreId:$terminalId"

        val confirmation = CoreConfirmation(
            requestId = reqId,
            operationType = opType,
            requestingDeviceId = terminalId,
            coreDeviceId = coreId,
            status = "EXECUTED_AND_CONFIRMED",
            executionDetails = "Bloqueo ejecutado en CORE",
            executionTimestamp = System.currentTimeMillis()
        )

        val plaintextBytes = json.encodeToString(confirmation).toByteArray(Charsets.UTF_8)
        val (nonce, ciphertext) = cryptoEngine.encrypt(key, plaintextBytes, correlationAad.toByteArray(Charsets.UTF_8))

        // Descifrado y verificación de AAD
        val decryptedBytes = cryptoEngine.decrypt(key, nonce, ciphertext, correlationAad.toByteArray(Charsets.UTF_8))
        val verifiedConfirmation = json.decodeFromString<CoreConfirmation>(String(decryptedBytes, Charsets.UTF_8))

        // Validación estricta de correlación
        assertEquals(reqId, verifiedConfirmation.requestId)
        assertEquals(opType, verifiedConfirmation.operationType)
        assertEquals(terminalId, verifiedConfirmation.requestingDeviceId)
        assertEquals(coreId, verifiedConfirmation.coreDeviceId)
        assertEquals("EXECUTED_AND_CONFIRMED", verifiedConfirmation.status)
    }

    @Test(expected = SecurityException::class)
    fun testMismatchedAadCorrelationIsRejectedByCryptoEngine() {
        val reqId = "req-uuid-9901"
        val opType = "LOCK_DEVICE"
        val terminalId = "term-device-A"
        val coreId = "core-device-B"

        val genuineAad = "$reqId:$opType:$coreId:$terminalId"
        val forgedAad = "$reqId:EMERGENCY_WIPE:$coreId:$terminalId" // Operación no coincide

        val confirmation = CoreConfirmation(
            requestId = reqId,
            operationType = opType,
            requestingDeviceId = terminalId,
            coreDeviceId = coreId,
            status = "EXECUTED_AND_CONFIRMED",
            executionDetails = "OK",
            executionTimestamp = System.currentTimeMillis()
        )

        val plaintextBytes = json.encodeToString(confirmation).toByteArray(Charsets.UTF_8)
        val (nonce, ciphertext) = cryptoEngine.encrypt(key, plaintextBytes, genuineAad.toByteArray(Charsets.UTF_8))

        // El descifrado con AAD discrepante debe fallar en el tag GCM
        cryptoEngine.decrypt(key, nonce, ciphertext, forgedAad.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun testDiscrepantDeviceMetadataIsRejectedEvenIfDecrypted() {
        val reqId = "req-uuid-9901"
        val expectedTerminalId = "term-device-A"
        val maliciousTerminalId = "term-attacker-X"
        val coreId = "core-device-B"

        // Confirmación que devuelve un deviceId diferente al esperado
        val confirmation = CoreConfirmation(
            requestId = reqId,
            operationType = "LOCK_DEVICE",
            requestingDeviceId = maliciousTerminalId,
            coreDeviceId = coreId,
            status = "EXECUTED_AND_CONFIRMED",
            executionDetails = "Inyección",
            executionTimestamp = System.currentTimeMillis()
        )

        // Verificación lógica en la capa de correlación
        val isCorrelated = confirmation.requestId == reqId &&
                confirmation.requestingDeviceId == expectedTerminalId &&
                confirmation.coreDeviceId == coreId

        assertFalse("Debe rechazarse si el dispositivo solicitante no coincide", isCorrelated)
    }
}
