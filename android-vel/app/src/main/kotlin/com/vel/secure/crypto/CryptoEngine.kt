package com.vel.secure.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Motor Criptográfico Nativo de VEL.
 * Implementa AES-256 en modo GCM con javax.crypto conforme a las especificaciones Parte VI (21, 22, 23).
 *
 * - Algoritmo: AES/GCM/NoPadding
 * - Longitud de clave: 256 bits (32 bytes)
 * - Nonce/IV: 96 bits (12 bytes) aleatorio y único por operación
 * - Etiqueta de autenticación: 128 bits
 * - Soporte para Datos Autenticados Adicionales (AAD)
 */
class CryptoEngine(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    companion object {
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12       // 96 bits
        const val TAG_BITS = 128          // 128 bits
        const val KEY_BYTES = 32          // 256 bits
    }

    /**
     * Cifra un contenido utilizando AES-GCM con un nonce aleatorio de 96 bits.
     * @param key Clave simétrica de 256 bits derivada de la sesión
     * @param plaintext Bytes del mensaje en texto claro
     * @param aad Datos autenticados adicionales opcionales (ej: header del protocolo, id de solicitud)
     * @return Par de datos: (nonce de 12 bytes, ciphertext que incluye el tag GCM de 16 bytes al final)
     */
    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): Pair<ByteArray, ByteArray> {
        require(key.size == KEY_BYTES) { "La clave AES debe ser de 256 bits (${KEY_BYTES} bytes)" }

        val nonce = ByteArray(NONCE_BYTES)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_BITS, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        aad?.let { cipher.updateAAD(it) }

        val ciphertext = cipher.doFinal(plaintext)
        return Pair(nonce, ciphertext)
    }

    /**
     * Descifra y autentica un mensaje en formato AES-GCM.
     * Lanza SecurityException si la etiqueta de autenticación o el AAD han sido manipulados.
     */
    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == KEY_BYTES) { "La clave AES debe ser de 256 bits" }
        require(nonce.size == NONCE_BYTES) { "El nonce GCM debe tener 96 bits (12 bytes)" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_BITS, nonce)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        aad?.let { cipher.updateAAD(it) }

        return try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw SecurityException("Fallo criptográfico: autenticidad del mensaje violada o datos manipulados", e)
        }
    }
}
