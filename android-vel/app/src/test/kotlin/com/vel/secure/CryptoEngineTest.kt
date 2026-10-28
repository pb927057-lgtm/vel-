package com.vel.secure

import com.vel.secure.crypto.CryptoEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * Pruebas unitarias de cifrado E2EE AES-GCM (Parte XIV.43).
 */
class CryptoEngineTest {
    private lateinit var cryptoEngine: CryptoEngine
    private val key = ByteArray(32)

    @Before
    fun setUp() {
        cryptoEngine = CryptoEngine()
        SecureRandom().nextBytes(key)
    }

    @Test
    fun testEncryptionAndDecryptionSucceeds() {
        val originalText = "COMANDO_PROTEGIDO_LOCK_DOWN_VEL".toByteArray(Charsets.UTF_8)
        val aad = "REQUEST_ID_987654".toByteArray(Charsets.UTF_8)

        val (nonce, ciphertext) = cryptoEngine.encrypt(key, originalText, aad)

        assertNotNull(nonce)
        assertEquals(12, nonce.size) // 96 bits
        assertNotNull(ciphertext)
        assertTrue(ciphertext.size >= originalText.size + 16) // Incluye tag GCM de 128 bits

        val decrypted = cryptoEngine.decrypt(key, nonce, ciphertext, aad)
        val resultString = String(decrypted, Charsets.UTF_8)

        assertEquals("COMANDO_PROTEGIDO_LOCK_DOWN_VEL", resultString)
    }

    @Test(expected = SecurityException::class)
    fun testTamperedCiphertextIsRejected() {
        val originalText = "OPERACION_SENSIBLE".toByteArray(Charsets.UTF_8)
        val (nonce, ciphertext) = cryptoEngine.encrypt(key, originalText)

        // Manipular 1 byte del ciphertext (ataque de integridad)
        ciphertext[0] = (ciphertext[0].toInt() xor 0xFF).toByte()

        // Debe lanzar SecurityException inmediatamente al fallar el tag GCM
        cryptoEngine.decrypt(key, nonce, ciphertext)
    }

    @Test(expected = SecurityException::class)
    fun testTamperedAadIsRejected() {
        val originalText = "OPERACION_SENSIBLE".toByteArray(Charsets.UTF_8)
        val validAad = "CORRECT_HEADER".toByteArray(Charsets.UTF_8)
        val forgedAad = "TAMPERED_HEADER".toByteArray(Charsets.UTF_8)

        val (nonce, ciphertext) = cryptoEngine.encrypt(key, originalText, validAad)

        // Descifrar con AAD falsificado debe fallar la verificación de autenticidad
        cryptoEngine.decrypt(key, nonce, ciphertext, forgedAad)
    }

    @Test
    fun testNoncesAreUniqueAcrossMultipleEncryptions() {
        val message = "TEST_PAYLOAD".toByteArray(Charsets.UTF_8)
        val nonces = HashSet<String>()

        for (i in 0 until 100) {
            val (nonce, _) = cryptoEngine.encrypt(key, message)
            val hex = nonce.joinToString("") { "%02x".format(it) }
            assertFalse("El nonce no debe repetirse jamás", nonces.contains(hex))
            nonces.add(hex)
        }
    }
}
