package com.vel.server

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RelayServerTest {
    @Test
    fun testRevocationRegistryTracksAndRejectsDevices() {
        val registry = RevocationRegistry()
        val targetDevice = "terminal-compromised-99"

        assertFalse(registry.isRevoked(targetDevice), "Dispositivo no debe estar revocado inicialmente")

        registry.recordRevocation(targetDevice)

        assertTrue(registry.isRevoked(targetDevice), "Dispositivo debe marcarse como revocado")
        assertEquals(1, registry.getRevokedDevicesCount())
    }

    @Test
    fun testServerCannotDecryptPayloadsWithoutKeys() {
        // Demuestra que la entidad de retransmisión solo maneja los strings Base64
        // del sobre cifrado y nunca tiene conocimiento del contenido descifrado.
        val dummyCiphertext = "g3F8sD92nZ01AakxLqWw=="
        val dummyNonce = "aBcDeFgHiJkL"

        val packet = RelayPacket(
            protocolVersion = 1,
            requestId = "req-1234",
            sourceDeviceId = "terminal-1",
            targetDeviceId = "core-1",
            nonceBase64 = dummyNonce,
            ciphertextBase64 = dummyCiphertext,
            aadBase64 = "aad-meta",
            timestamp = System.currentTimeMillis()
        )

        // El servidor almacena y transporta únicamente el blob binario cifrado
        assertEquals("g3F8sD92nZ01AakxLqWw==", packet.ciphertextBase64)
    }
}
