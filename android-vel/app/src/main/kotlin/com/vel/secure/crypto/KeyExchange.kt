package com.vel.secure.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Protocolo de intercambio de claves autenticado con curva elíptica P-256 (ECDH)
 * y derivación de claves de sesión direccionales mediante HKDF (RFC 5869) con HMAC-SHA-256.
 */
object KeyExchange {
    private const val EC_CURVE = "secp256r1"
    private const val HMAC_ALGO = "HmacSHA256"

    /**
     * Genera un par de claves efímeras para el intercambio ECDH en el dispositivo.
     */
    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec(EC_CURVE)
        kpg.initialize(ecSpec)
        return kpg.generateKeyPair()
    }

    /**
     * Reconstruye una clave pública desde su codificación X.509
     */
    fun decodePublicKey(encoded: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance("EC")
        val keySpec = X509EncodedKeySpec(encoded)
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Calcula el secreto compartido Z = ECDH(localPrivateKey, remotePublicKey)
     */
    fun computeSharedSecret(localKeyPair: KeyPair, remotePublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(localKeyPair.private)
        keyAgreement.doPhase(remotePublicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * HKDF-Extract(salt, IKM) -> PRK
     */
    fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGO)
        val saltKey = SecretKeySpec(if (salt.isNotEmpty()) salt else ByteArray(32), HMAC_ALGO)
        mac.init(saltKey)
        return mac.doFinal(ikm)
    }

    /**
     * HKDF-Expand(PRK, info, L) -> OKM
     */
    fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGO)
        val prkKey = SecretKeySpec(prk, HMAC_ALGO)
        mac.init(prkKey)

        var t = ByteArray(0)
        val okm = ByteArray(length)
        var offset = 0
        var i = 1

        while (offset < length) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()

            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, okm, offset, toCopy)
            offset += toCopy
            i++
        }

        return okm
    }

    /**
     * Deriva las claves de sesión simétricas direccionales para la conexión:
     * - Clave Terminal -> CORE (envío de operaciones protegidas)
     * - Clave CORE -> Terminal (confirmaciones autenticadas de ejecución)
     */
    fun deriveSessionKeys(
        sharedSecret: ByteArray,
        salt: ByteArray,
        pairingContext: String
    ): Pair<ByteArray, ByteArray> {
        val prk = hkdfExtract(salt, sharedSecret)

        val terminalToCoreInfo = "VEL-v1-terminal-to-core:$pairingContext".toByteArray(Charsets.UTF_8)
        val coreToTerminalInfo = "VEL-v1-core-to-terminal:$pairingContext".toByteArray(Charsets.UTF_8)

        val terminalToCoreKey = hkdfExpand(prk, terminalToCoreInfo, 32)
        val coreToTerminalKey = hkdfExpand(prk, coreToTerminalInfo, 32)

        return Pair(terminalToCoreKey, coreToTerminalKey)
    }
}
