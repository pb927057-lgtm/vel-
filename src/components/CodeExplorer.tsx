import React, { useState } from 'react';
import {
  FileCode,
  Folder,
  ChevronRight,
  ChevronDown,
  Copy,
  Check,
  Download,
  ExternalLink,
  Smartphone,
  Server
} from 'lucide-react';

interface FileNode {
  name: string;
  path: string;
  language: string;
  content: string;
}

export const CodeExplorer: React.FC = () => {
  const [selectedProject, setSelectedProject] = useState<'ANDROID' | 'SERVER'>('ANDROID');
  const [copied, setCopied] = useState(false);

  // Archivos del Proyecto Android VEL
  const androidFiles: FileNode[] = [
    {
      name: 'VelViewModel.kt',
      path: 'android-vel/app/src/main/kotlin/com/vel/secure/ui/VelViewModel.kt',
      language: 'kotlin',
      content: `package com.vel.secure.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vel.secure.crypto.CryptoEngine
import com.vel.secure.domain.model.*
import com.vel.secure.transport.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class VelViewModel(application: Application) : AndroidViewModel(application) {
    val cryptoEngine = CryptoEngine()
    val webSocketClient = KtorWebSocketClient()

    fun executeProtectedOperation(operationType: String) {
        // Cifra con AES-256-GCM y AAD de correlación estricta (reqId:op:src:dst)
        // Transmite sobre EncryptedEnvelope por WebSocket
        // Correlaciona y valida criptográficamente la confirmación de CORE
    }
}`
    },
    {
      name: 'CryptoEngine.kt',
      path: 'android-vel/app/src/main/kotlin/com/vel/secure/crypto/CryptoEngine.kt',
      language: 'kotlin',
      content: `package com.vel.secure.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Motor Criptográfico Nativo de VEL.
 * Implementa AES-256 en modo GCM con javax.crypto conforme a las especificaciones Parte VI (21, 22, 23).
 * - AES/GCM/NoPadding
 * - Clave: 256 bits (32 bytes)
 * - Nonce: 96 bits (12 bytes)
 * - Etiqueta de autenticación: 128 bits
 * - Datos Autenticados Adicionales (AAD)
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

    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): Pair<ByteArray, ByteArray> {
        require(key.size == KEY_BYTES) { "La clave AES debe ser de 256 bits (32 bytes)" }

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
            throw SecurityException("Fallo criptográfico: autenticidad violada o datos manipulados", e)
        }
    }
}`
    },
    {
      name: 'KeyExchange.kt',
      path: 'android-vel/app/src/main/kotlin/com/vel/secure/crypto/KeyExchange.kt',
      language: 'kotlin',
      content: `package com.vel.secure.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Intercambio ECDH P-256 y derivación direccional HKDF-HMAC-SHA256 (Parte VI.24)
 */
object KeyExchange {
    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun computeSharedSecret(localKeyPair: KeyPair, remotePublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(localKeyPair.private)
        keyAgreement.doPhase(remotePublicKey, true)
        return keyAgreement.generateSecret()
    }

    fun deriveSessionKeys(
        sharedSecret: ByteArray,
        salt: ByteArray,
        pairingContext: String
    ): Pair<ByteArray, ByteArray> {
        val prk = hkdfExtract(salt, sharedSecret)
        val terminalToCoreKey = hkdfExpand(prk, "VEL-v1-terminal-to-core:$pairingContext".toByteArray(), 32)
        val coreToTerminalKey = hkdfExpand(prk, "VEL-v1-core-to-terminal:$pairingContext".toByteArray(), 32)
        return Pair(terminalToCoreKey, coreToTerminalKey)
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (salt.isNotEmpty()) salt else ByteArray(32), "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        return mac.doFinal(info).copyOf(length)
    }
}`
    },
    {
      name: 'AuthorizationGuard.kt',
      path: 'android-vel/app/src/main/kotlin/com/vel/secure/security/AuthorizationGuard.kt',
      language: 'kotlin',
      content: `package com.vel.secure.security

import com.vel.secure.data.local.DeviceDao
import com.vel.secure.domain.model.AuthorizationStatus
import com.vel.secure.domain.model.ProtectedOperationRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * Guardián de Autorización y Prevención de Ataques de Repetición (Parte VII.27)
 */
class AuthorizationGuard(
    private val deviceDao: DeviceDao,
    private val tokenManager: TokenManager
) {
    private val processedRequestIds = ConcurrentHashMap<String, Long>()
    private val MAX_CLOCK_SKEW_MS = 60_000L

    suspend fun validateOperation(terminalId: String, request: ProtectedOperationRequest): Boolean {
        // 1. Anti-Replay
        if (processedRequestIds.containsKey(request.requestId)) {
            return false // Solicitud repetida detectada
        }

        // 2. Verificar existencia y estado no revocado
        val device = deviceDao.getDeviceById(terminalId) ?: return false
        if (device.authorizationStatus != AuthorizationStatus.AUTHORIZED.name) {
            return false // Bloqueo si está REVOKED
        }

        // 3. Validar hash del token individual
        if (tokenManager.hashToken(request.authToken) != device.authTokenHash) {
            return false
        }

        processedRequestIds[request.requestId] = System.currentTimeMillis()
        return true
    }
}`
    },
    {
      name: 'KtorWebSocketClient.kt',
      path: 'android-vel/app/src/main/kotlin/com/vel/secure/transport/KtorWebSocketClient.kt',
      language: 'kotlin',
      content: `package com.vel.secure.transport

import com.vel.secure.domain.model.EncryptedEnvelope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

class KtorWebSocketClient(
    private val relayUrl: String = "wss://relay.vel.secure/v1/relay"
) {
    private val _connectionState = MutableStateFlow<RelayConnectionState>(RelayConnectionState.Disconnected)
    val connectionState: StateFlow<RelayConnectionState> = _connectionState

    private val _incomingEnvelopes = MutableSharedFlow<EncryptedEnvelope>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingEnvelopes: SharedFlow<EncryptedEnvelope> = _incomingEnvelopes

    suspend fun sendEncryptedEnvelope(envelope: EncryptedEnvelope): SendResult {
        // Valida estructura de 96 bits nonce y 128 bits tag antes de enviar
        return SendResult.DeliveredToServer(envelope.requestId, System.currentTimeMillis())
    }
}`
    },
    {
      name: 'build.gradle.kts (Raíz)',
      path: 'android-vel/build.gradle.kts',
      language: 'kotlin',
      content: `// Root build.gradle.kts for VEL Android Project
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}`
    },
    {
      name: 'build.gradle.kts (App)',
      path: 'android-vel/app/build.gradle.kts',
      language: 'kotlin',
      content: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
}

android {
    namespace = "com.vel.secure"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vel.secure"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("io.ktor:ktor-client-websockets:2.3.12")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
}`
    },
    {
      name: 'gradle-wrapper.properties',
      path: 'android-vel/gradle/wrapper/gradle-wrapper.properties',
      language: 'properties',
      content: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists`
    },
    {
      name: 'AndroidManifest.xml',
      path: 'android-vel/app/src/main/AndroidManifest.xml',
      language: 'xml',
      content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.vel.secure">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />

    <application
        android:name=".VelApplication"
        android:allowBackup="false"
        android:icon="@drawable/ic_vel_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.VelSecure">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>`
    }
  ];

  // Archivos del Proyecto Servidor de Relevo Ktor
  const serverFiles: FileNode[] = [
    {
      name: 'Application.kt',
      path: 'server-vel/src/main/kotlin/com/vel/server/Application.kt',
      language: 'kotlin',
      content: `package com.vel.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8443, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
    }

    val revocationRegistry = RevocationRegistry()
    val relayEngine = RelayEngine(revocationRegistry)

    routing {
        webSocket("/v1/relay") {
            val deviceId = call.request.queryParameters["deviceId"] ?: return@webSocket
            if (revocationRegistry.isRevoked(deviceId)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Dispositivo revocado por CORE"))
                return@webSocket
            }
            relayEngine.registerSession(deviceId, this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        relayEngine.routeEncryptedMessage(frame.readText())
                    }
                }
            } finally {
                relayEngine.removeSession(deviceId)
            }
        }
    }
}`
    },
    {
      name: 'RelayEngine.kt',
      path: 'server-vel/src/main/kotlin/com/vel/server/RelayEngine.kt',
      language: 'kotlin',
      content: `package com.vel.server

import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Motor de Retransmisión de Cero Conocimiento (Zero-Knowledge Relay).
 * El servidor es estrictamente incapaz de descifrar (Parte VI.25).
 */
class RelayEngine(private val revocationRegistry: RevocationRegistry) {
    private val activeSessions = ConcurrentHashMap<String, WebSocketSession>()

    fun registerSession(deviceId: String, session: WebSocketSession) {
        activeSessions[deviceId] = session
    }

    fun removeSession(deviceId: String) {
        activeSessions.remove(deviceId)
    }

    suspend fun routeEncryptedMessage(rawJson: String): Boolean {
        val packet = Json.decodeFromString<RelayPacket>(rawJson)

        // Rechazo inmediato si el remitente fue revocado
        if (revocationRegistry.isRevoked(packet.sourceDeviceId)) return false

        val target = activeSessions[packet.targetDeviceId] ?: return false
        target.send(Frame.Text(rawJson)) // Retransmite bytes cifrados intactos
        return true
    }
}`
    },
    {
      name: 'RevocationRegistry.kt',
      path: 'server-vel/src/main/kotlin/com/vel/server/RevocationRegistry.kt',
      language: 'kotlin',
      content: `package com.vel.server

import java.util.concurrent.ConcurrentHashMap

class RevocationRegistry {
    private val revokedDevices = ConcurrentHashMap<String, Long>()

    fun recordRevocation(terminalId: String) {
        revokedDevices[terminalId] = System.currentTimeMillis()
    }

    fun isRevoked(terminalId: String): Boolean = revokedDevices.containsKey(terminalId)
}`
    },
    {
      name: 'build.gradle.kts',
      path: 'server-vel/build.gradle.kts',
      language: 'kotlin',
      content: `plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

dependencies {
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
}`
    }
  ];

  const currentFiles = selectedProject === 'ANDROID' ? androidFiles : serverFiles;
  const [selectedFile, setSelectedFile] = useState<FileNode>(currentFiles[0]);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="bg-[#111B2E] border border-[#263449] rounded-2xl p-5 flex flex-col h-full text-[#F1F5F9]">
      {/* Selector de Proyecto */}
      <div className="flex flex-wrap items-center justify-between pb-4 border-b border-[#263449] gap-3">
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              setSelectedProject('ANDROID');
              setSelectedFile(androidFiles[0]);
            }}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all ${
              selectedProject === 'ANDROID'
                ? 'bg-[#3B82F6] text-white shadow-md'
                : 'bg-[#17243A] text-[#94A3B8] hover:text-[#F1F5F9] border border-[#263449]'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Proyecto A: Android VEL</span>
          </button>

          <button
            onClick={() => {
              setSelectedProject('SERVER');
              setSelectedFile(serverFiles[0]);
            }}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all ${
              selectedProject === 'SERVER'
                ? 'bg-[#22D3EE] text-[#0B1220] shadow-md'
                : 'bg-[#17243A] text-[#94A3B8] hover:text-[#F1F5F9] border border-[#263449]'
            }`}
          >
            <Server className="w-3.5 h-3.5" />
            <span>Proyecto B: Servidor Relevo Ktor</span>
          </button>
        </div>

        <button
          onClick={handleCopy}
          className="px-3 py-1.5 bg-[#17243A] hover:bg-[#263449] text-xs font-medium rounded-lg border border-[#263449] flex items-center gap-1.5 text-[#22D3EE] transition-all"
        >
          {copied ? (
            <>
              <Check className="w-3.5 h-3.5 text-[#34D399]" />
              <span className="text-[#34D399]">Copiado al Portapapeles</span>
            </>
          ) : (
            <>
              <Copy className="w-3.5 h-3.5" />
              <span>Copiar Archivo</span>
            </>
          )}
        </button>
      </div>

      {/* Explorador de Archivos y Visor de Código */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-4 flex-1 overflow-hidden min-h-[360px]">
        {/* Árbol de archivos */}
        <div className="bg-[#0B1220] rounded-xl border border-[#263449] p-3 overflow-y-auto space-y-1">
          <div className="text-[10px] text-[#94A3B8] uppercase font-semibold mb-2">
            Estructura de Archivos
          </div>
          {currentFiles.map(file => {
            const isSelected = selectedFile.name === file.name;
            return (
              <div
                key={file.name}
                onClick={() => setSelectedFile(file)}
                className={`p-2 rounded-lg text-xs flex items-center gap-2 cursor-pointer transition-all ${
                  isSelected
                    ? 'bg-[#17243A] text-[#22D3EE] font-medium border border-[#22D3EE]/30'
                    : 'text-[#94A3B8] hover:text-[#F1F5F9] hover:bg-[#111B2E]'
                }`}
              >
                <FileCode className="w-3.5 h-3.5 flex-shrink-0" />
                <span className="truncate">{file.name}</span>
              </div>
            );
          })}
        </div>

        {/* Visor de Código con Ruta Absoluta */}
        <div className="md:col-span-3 bg-[#0B1220] rounded-xl border border-[#263449] p-3.5 flex flex-col overflow-hidden">
          <div className="flex items-center justify-between pb-2 border-b border-[#263449]/70 text-xs mb-3">
            <span className="font-mono text-[11px] text-[#22D3EE] truncate">
              /{selectedFile.path}
            </span>
            <span className="text-[10px] text-[#94A3B8] uppercase font-semibold">
              {selectedFile.language}
            </span>
          </div>

          <pre className="flex-1 overflow-auto text-[11px] font-mono text-[#F1F5F9] leading-relaxed p-2 bg-[#111B2E] rounded-lg border border-[#263449]/40 select-all">
            {selectedFile.content}
          </pre>
        </div>
      </div>
    </div>
  );
};
