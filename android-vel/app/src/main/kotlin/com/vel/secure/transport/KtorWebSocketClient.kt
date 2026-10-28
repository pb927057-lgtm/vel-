package com.vel.secure.transport

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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

sealed class RelayConnectionState {
    object Disconnected : RelayConnectionState()
    object Connecting : RelayConnectionState()
    data class Connected(val relayEndpoint: String) : RelayConnectionState()
    data class Reconnecting(val attempt: Int, val nextRetryMs: Long) : RelayConnectionState()
    data class Error(val message: String, val isRecoverable: Boolean) : RelayConnectionState()
}

sealed class SendResult {
    /**
     * El sobre cifrado fue transmitido exitosamente por el socket al servidor de relevo.
     * ADVERTENCIA DE SEGURIDAD: Esto únicamente acredita la entrega al transporte; NO constituye
     * confirmación de ejecución de CORE. La confirmación solo es válida cuando se reciba y
     * verifique criptográficamente el sobre de respuesta de CORE con el mismo requestId.
     */
    data class DeliveredToServer(val requestId: String, val timestamp: Long) : SendResult()
    data class Failed(val reason: String) : SendResult()
}

sealed class EnvelopeValidation {
    object Valid : EnvelopeValidation()
    data class Invalid(val reason: String) : EnvelopeValidation()
}

/**
 * Cliente Ktor WebSocket de VEL.
 * Gestiona la conexión y retransmisión de sobres cifrados E2EE entre dispositivos
 * a través del servidor de relevo de Cero Conocimiento.
 */
class KtorWebSocketClient(
    var relayUrl: String = "ws://10.0.2.2:8443/v1/relay",
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
            maxFrameSize = 65_536
        }
    }
) {
    fun updateRelayUrl(newUrl: String) {
        relayUrl = newUrl
    }
    companion object {
        const val MAX_PAYLOAD_BYTES = 65_536 // 64 KB límite máximo de sobre
        const val EXPECTED_NONCE_BYTES = 12  // 96 bits exactos
        const val MIN_CIPHERTEXT_BYTES = 16  // Tag GCM de 128 bits mínimo
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val INITIAL_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 16_000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    private val _connectionState = MutableStateFlow<RelayConnectionState>(RelayConnectionState.Disconnected)
    val connectionState: StateFlow<RelayConnectionState> = _connectionState.asStateFlow()

    // Flujo tipado de sobres cifrados entrantes previamente validados en formato
    private val _incomingEnvelopes = MutableSharedFlow<EncryptedEnvelope>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingEnvelopes: SharedFlow<EncryptedEnvelope> = _incomingEnvelopes.asSharedFlow()

    private var activeSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var manualDisconnectRequested: Boolean = false

    /**
     * Inicia el bucle de conexión en una corrutina con política de reconexión controlada.
     */
    fun startConnection(
        scope: CoroutineScope,
        deviceId: String,
        role: String,
        authToken: String
    ) {
        manualDisconnectRequested = false
        connectionJob?.cancel()
        connectionJob = scope.launch {
            connectWithRetryLoop(deviceId, role, authToken)
        }
    }

    private suspend fun connectWithRetryLoop(deviceId: String, role: String, authToken: String) {
        var attempt = 0
        var backoffMs = INITIAL_BACKOFF_MS

        while (!manualDisconnectRequested) {
            _connectionState.value = RelayConnectionState.Connecting
            try {
                val fullUrl = "$relayUrl?deviceId=$deviceId&role=$role&auth=$authToken"
                client.webSocket(urlString = fullUrl) {
                    activeSession = this
                    attempt = 0
                    backoffMs = INITIAL_BACKOFF_MS
                    _connectionState.value = RelayConnectionState.Connected(relayUrl)

                    listenIncomingFrames()
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                if (manualDisconnectRequested) break

                attempt++
                if (attempt > MAX_RECONNECT_ATTEMPTS) {
                    _connectionState.value = RelayConnectionState.Error(
                        message = "Límite de reintentos alcanzado ($MAX_RECONNECT_ATTEMPTS). Servidor inaccesible: ${e.message}",
                        isRecoverable = false
                    )
                    break
                } else {
                    _connectionState.value = RelayConnectionState.Reconnecting(attempt, backoffMs)
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            } finally {
                activeSession = null
                if (manualDisconnectRequested) {
                    _connectionState.value = RelayConnectionState.Disconnected
                }
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.listenIncomingFrames() {
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val rawText = frame.readText()
                    processRawIncomingText(rawText)
                }
                is Frame.Close -> {
                    val reason = frame.readReason()
                    if (reason?.code == CloseReason.Codes.VIOLATED_POLICY.code) {
                        _connectionState.value = RelayConnectionState.Error(
                            message = "Conexión rechazada por el servidor: ${reason.message}",
                            isRecoverable = false
                        )
                        manualDisconnectRequested = true
                    }
                }
                else -> { /* No se admiten frames binarios fuera del sobre */ }
            }
        }
    }

    private suspend fun processRawIncomingText(rawText: String) {
        if (rawText.length > MAX_PAYLOAD_BYTES) {
            // Rechazar mensajes que excedan el límite técnico de tamaño
            return
        }

        val envelope = try {
            json.decodeFromString<EncryptedEnvelope>(rawText)
        } catch (e: Exception) {
            return
        }

        // Validación estricta de estructura antes de emitir al flujo de la aplicación
        val validation = validateEnvelope(envelope)
        if (validation is EnvelopeValidation.Valid) {
            _incomingEnvelopes.emit(envelope)
        }
    }

    /**
     * Valida la estructura técnica del sobre sin acceder ni asumir descifrado del contenido sensible.
     */
    fun validateEnvelope(envelope: EncryptedEnvelope): EnvelopeValidation {
        if (envelope.protocolVersion != 1) {
            return EnvelopeValidation.Invalid("Versión de protocolo no soportada: ${envelope.protocolVersion}")
        }
        if (envelope.requestId.isBlank()) {
            return EnvelopeValidation.Invalid("Identificador de solicitud (requestId) vacío")
        }
        if (envelope.sourceDeviceId.isBlank() || envelope.targetDeviceId.isBlank()) {
            return EnvelopeValidation.Invalid("Identificadores de origen o destino vacíos")
        }

        // Validar nonce GCM (debe tener exactamente 12 bytes = 96 bits)
        val nonceBytes = try {
            Base64.getDecoder().decode(envelope.nonceBase64)
        } catch (e: Exception) {
            return EnvelopeValidation.Invalid("Nonce Base64 inválido")
        }
        if (nonceBytes.size != EXPECTED_NONCE_BYTES) {
            return EnvelopeValidation.Invalid("Nonce inválido: se esperaban $EXPECTED_NONCE_BYTES bytes y se recibieron ${nonceBytes.size}")
        }

        // Validar ciphertext GCM (debe contener al menos el tag de 16 bytes = 128 bits)
        val ciphertextBytes = try {
            Base64.getDecoder().decode(envelope.ciphertextBase64)
        } catch (e: Exception) {
            return EnvelopeValidation.Invalid("Ciphertext Base64 inválido")
        }
        if (ciphertextBytes.size < MIN_CIPHERTEXT_BYTES) {
            return EnvelopeValidation.Invalid("Ciphertext corrupto: menor al tamaño de la etiqueta de autenticación")
        }

        return EnvelopeValidation.Valid
    }

    /**
     * Envía un sobre cifrado previamente construido y validado por el emisor.
     */
    suspend fun sendEncryptedEnvelope(envelope: EncryptedEnvelope): SendResult {
        val session = activeSession
            ?: return SendResult.Failed("No existe una sesión WebSocket activa con el servidor de relevo")

        val validation = validateEnvelope(envelope)
        if (validation is EnvelopeValidation.Invalid) {
            return SendResult.Failed("Sobre inválido para transmisión: ${validation.reason}")
        }

        return try {
            val jsonPayload = json.encodeToString(envelope)
            if (jsonPayload.length > MAX_PAYLOAD_BYTES) {
                return SendResult.Failed("El tamaño del sobre (${jsonPayload.length} bytes) excede el máximo permitido ($MAX_PAYLOAD_BYTES bytes)")
            }

            session.send(Frame.Text(jsonPayload))
            SendResult.DeliveredToServer(envelope.requestId, System.currentTimeMillis())
        } catch (e: Exception) {
            SendResult.Failed("Error de transmisión por socket: ${e.message}")
        }
    }

    suspend fun disconnect() {
        manualDisconnectRequested = true
        connectionJob?.cancel()
        activeSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Desconexión manual de cliente"))
        activeSession = null
        _connectionState.value = RelayConnectionState.Disconnected
    }
}
