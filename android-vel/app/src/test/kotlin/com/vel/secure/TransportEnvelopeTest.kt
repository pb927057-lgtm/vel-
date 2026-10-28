package com.vel.secure

import com.vel.secure.domain.model.EncryptedEnvelope
import com.vel.secure.transport.EnvelopeValidation
import com.vel.secure.transport.KtorWebSocketClient
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class TransportEnvelopeTest {
    private val client = KtorWebSocketClient()

    @Test
    fun testValidEnvelopePassesValidation() {
        val validNonce = Base64.getEncoder().encodeToString(ByteArray(12)) // 96 bits
        val validCiphertext = Base64.getEncoder().encodeToString(ByteArray(32)) // > 16 bytes

        val envelope = EncryptedEnvelope(
            protocolVersion = 1,
            requestId = "req-12345",
            sourceDeviceId = "terminal-01",
            targetDeviceId = "core-01",
            nonceBase64 = validNonce,
            ciphertextBase64 = validCiphertext,
            aadBase64 = "",
            timestamp = System.currentTimeMillis()
        )

        val result = client.validateEnvelope(envelope)
        assertTrue(result is EnvelopeValidation.Valid)
    }

    @Test
    fun testEnvelopeWithInvalidNonceLengthIsRejected() {
        // Nonce de 8 bytes en lugar de 12 bytes
        val shortNonce = Base64.getEncoder().encodeToString(ByteArray(8))
        val validCiphertext = Base64.getEncoder().encodeToString(ByteArray(32))

        val envelope = EncryptedEnvelope(
            protocolVersion = 1,
            requestId = "req-12345",
            sourceDeviceId = "terminal-01",
            targetDeviceId = "core-01",
            nonceBase64 = shortNonce,
            ciphertextBase64 = validCiphertext,
            aadBase64 = "",
            timestamp = System.currentTimeMillis()
        )

        val result = client.validateEnvelope(envelope)
        assertTrue(result is EnvelopeValidation.Invalid)
        assertTrue((result as EnvelopeValidation.Invalid).reason.contains("Nonce inválido"))
    }

    @Test
    fun testEnvelopeWithShortCiphertextIsRejected() {
        // Ciphertext de 8 bytes (menor que el tag GCM de 16 bytes)
        val validNonce = Base64.getEncoder().encodeToString(ByteArray(12))
        val shortCiphertext = Base64.getEncoder().encodeToString(ByteArray(8))

        val envelope = EncryptedEnvelope(
            protocolVersion = 1,
            requestId = "req-12345",
            sourceDeviceId = "terminal-01",
            targetDeviceId = "core-01",
            nonceBase64 = validNonce,
            ciphertextBase64 = shortCiphertext,
            aadBase64 = "",
            timestamp = System.currentTimeMillis()
        )

        val result = client.validateEnvelope(envelope)
        assertTrue(result is EnvelopeValidation.Invalid)
        assertTrue((result as EnvelopeValidation.Invalid).reason.contains("Ciphertext corrupto"))
    }

    @Test
    fun testEnvelopeWithUnsupportedProtocolVersionIsRejected() {
        val validNonce = Base64.getEncoder().encodeToString(ByteArray(12))
        val validCiphertext = Base64.getEncoder().encodeToString(ByteArray(32))

        val envelope = EncryptedEnvelope(
            protocolVersion = 99, // Versión no soportada
            requestId = "req-12345",
            sourceDeviceId = "terminal-01",
            targetDeviceId = "core-01",
            nonceBase64 = validNonce,
            ciphertextBase64 = validCiphertext,
            aadBase64 = "",
            timestamp = System.currentTimeMillis()
        )

        val result = client.validateEnvelope(envelope)
        assertTrue(result is EnvelopeValidation.Invalid)
        assertTrue((result as EnvelopeValidation.Invalid).reason.contains("Versión de protocolo no soportada"))
    }
}
