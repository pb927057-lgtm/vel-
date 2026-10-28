package com.vel.secure.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

/**
 * Gestor de identidad criptográfica respaldado por Android Keystore.
 * Las claves privadas nunca salen del área de seguridad de hardware del dispositivo.
 */
class KeyStoreManager {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS = "vel_identity_root_key"
        private const val EC_CURVE = "secp256r1"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Obtiene o genera la identidad criptográfica asimétrica persistente del dispositivo.
     */
    fun getOrCreateDeviceIdentity(): PublicKey {
        if (!keyStore.containsAlias(IDENTITY_KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )

            val parameterSpec = KeyGenParameterSpec.Builder(
                IDENTITY_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false) // Puede elevarse con Biometrics según política
                .build()

            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }

        val certificate = keyStore.getCertificate(IDENTITY_KEY_ALIAS)
            ?: throw IllegalStateException("No se pudo obtener el certificado de identidad")
        return certificate.publicKey
    }

    /**
     * Elimina las claves almacenadas al revocar el dispositivo o restablecer el rol.
     */
    fun purgeIdentity() {
        if (keyStore.containsAlias(IDENTITY_KEY_ALIAS)) {
            keyStore.deleteEntry(IDENTITY_KEY_ALIAS)
        }
    }
}
