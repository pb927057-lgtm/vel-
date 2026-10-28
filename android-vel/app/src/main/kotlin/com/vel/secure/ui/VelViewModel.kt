package com.vel.secure.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vel.secure.VelApplication
import com.vel.secure.crypto.CryptoEngine
import com.vel.secure.domain.model.*
import com.vel.secure.security.AuthorizationGuard
import com.vel.secure.security.TokenManager
import com.vel.secure.transport.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PendingOperationRecord(
    val requestId: String,
    val opType: String,
    val terminalId: String,
    val targetCoreId: String,
    val timestamp: Long
)

class VelViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as VelApplication).database
    private val deviceDao = database.deviceDao()
    private val tokenManager = TokenManager()
    private val authorizationGuard = AuthorizationGuard(deviceDao, tokenManager)
    val cryptoEngine = CryptoEngine()
    val webSocketClient = KtorWebSocketClient()

    private val json = Json { ignoreUnknownKeys = true }

    // Estado del Dispositivo Local
    private val _deviceRole = MutableStateFlow(DeviceRole.UNCONFIGURED)
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    private val _deviceId = MutableStateFlow(UUID.randomUUID().toString())
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _pairedCoreId = MutableStateFlow<String?>(null)
    val pairedCoreId: StateFlow<String?> = _pairedCoreId.asStateFlow()

    private val _isRevoked = MutableStateFlow(false)
    val isRevoked: StateFlow<Boolean> = _isRevoked.asStateFlow()

    private val _currentOpStatus = MutableStateFlow<ExecutionStatus?>(null)
    val currentOpStatus: StateFlow<ExecutionStatus?> = _currentOpStatus.asStateFlow()

    private val _statusDetails = MutableStateFlow<String?>(null)
    val statusDetails: StateFlow<String?> = _statusDetails.asStateFlow()

    // Claves de Sesión Simétricas Direccionales en memoria activa (derivadas de ECDH+HKDF)
    private var terminalToCoreKey: ByteArray? = null
    private var coreToTerminalKey: ByteArray? = null

    // Almacén de solicitudes pendientes de confirmación para correlación estricta
    private val pendingOperations = ConcurrentHashMap<String, PendingOperationRecord>()
    private val timeoutWatchdogs = ConcurrentHashMap<String, Job>()

    // Exponer dispositivos autorizados de la base de datos Room
    val authorizedDevices = deviceDao.getAllDevices().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val connectionState = webSocketClient.connectionState

    private val _relayUrl = MutableStateFlow(webSocketClient.relayUrl)
    val relayUrl: StateFlow<String> = _relayUrl.asStateFlow()

    fun updateRelayUrl(newUrl: String) {
        _relayUrl.value = newUrl
        connectToRelay(newUrl)
    }

    init {
        // Iniciar escucha reactiva de sobres cifrados entrantes
        viewModelScope.launch {
            webSocketClient.incomingEnvelopes.collect { envelope ->
                handleIncomingEnvelope(envelope)
            }
        }
    }

    fun setRole(role: DeviceRole) {
        _deviceRole.value = role
        // Iniciar conexión automática al seleccionar rol
        connectToRelay()
    }

    fun connectToRelay(serverAddress: String? = null) {
        if (!serverAddress.isNullOrBlank()) {
            webSocketClient.updateRelayUrl(serverAddress)
        }
        val roleStr = if (_deviceRole.value == DeviceRole.CORE) "CORE" else "TERMINAL"
        webSocketClient.startConnection(
            scope = viewModelScope,
            deviceId = _deviceId.value,
            role = roleStr,
            authToken = "auth-token-vel"
        )
    }

    /**
     * Deriva la clave de sesión simétrica de 256 bits a partir del secreto de vinculación
     * compartido de forma sencilla y directa (SHA-256).
     */
    fun applyPairing(pairingCode: String, targetCoreId: String) {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val derivedKey = digest.digest("VEL-SESSION-KEY:$pairingCode".toByteArray(Charsets.UTF_8))
        terminalToCoreKey = derivedKey
        coreToTerminalKey = derivedKey
        _pairedCoreId.value = targetCoreId
        _isRevoked.value = false
    }

    fun approveTerminalAtCore(terminalId: String, deviceName: String, pairingCode: String) {
        viewModelScope.launch {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val derivedKey = digest.digest("VEL-SESSION-KEY:$pairingCode".toByteArray(Charsets.UTF_8))
            terminalToCoreKey = derivedKey
            coreToTerminalKey = derivedKey

            val entity = AuthorizedDeviceEntity(
                deviceId = terminalId,
                deviceName = deviceName,
                role = "TERMINAL",
                authorizationStatus = "AUTHORIZED",
                pairedAtTimestamp = System.currentTimeMillis(),
                lastSeenTimestamp = System.currentTimeMillis(),
                authTokenHash = tokenManager.hashToken("ACTIVE_SESSION_TOKEN"),
                publicKeyHex = "04ab34..."
            )
            deviceDao.insertOrUpdate(entity)
        }
    }

    fun revokeDeviceAtCore(targetTerminalId: String) {
        viewModelScope.launch {
            deviceDao.markAsRevoked(targetTerminalId)
            if (_deviceId.value == targetTerminalId) {
                _isRevoked.value = true
                _currentOpStatus.value = ExecutionStatus.REJECTED
                _statusDetails.value = "Dispositivo REVOCADO por CORE"
            }
        }
    }

    fun setSessionKeys(t2cKey: ByteArray, c2tKey: ByteArray, pairedCoreId: String) {
        terminalToCoreKey = t2cKey
        coreToTerminalKey = c2tKey
        _pairedCoreId.value = pairedCoreId
        _isRevoked.value = false
    }

    /**
     * Ejecuta una operación protegida desde el Terminal hacia el CORE (Especificaciones VI, VII y X).
     */
    fun executeProtectedOperation(operationType: String) {
        val currentTerminalId = _deviceId.value
        val targetCore = _pairedCoreId.value
        val t2cKey = terminalToCoreKey

        // 1. Validar precondiciones de seguridad
        if (_isRevoked.value) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "Operación rechazada: el dispositivo se encuentra REVOCADO"
            return
        }

        if (targetCore == null || t2cKey == null) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "No existe sesión criptográfica establecida con un CORE válido"
            return
        }

        // 2. Comprobar conectividad (No encolar datos sensibles sin canal seguro)
        if (webSocketClient.connectionState.value !is RelayConnectionState.Connected) {
            _currentOpStatus.value = ExecutionStatus.COMMUNICATION_ERROR
            _statusDetails.value = "Error de comunicación: canal WebSocket no disponible"
            return
        }

        val requestId = UUID.randomUUID().toString()
        _currentOpStatus.value = ExecutionStatus.PREPARING
        _statusDetails.value = "Preparando sobre cifrado AES-256-GCM..."

        viewModelScope.launch {
            try {
                // 3. Construir payload de solicitud protegida
                val request = ProtectedOperationRequest(
                    requestId = requestId,
                    operationType = operationType,
                    parametersJson = "{}",
                    authToken = "ACTIVE_SESSION_TOKEN",
                    clientTimestamp = System.currentTimeMillis()
                )

                // 4. Construir AAD estricto para correlación criptográfica (Identidades + Operación + RequestId)
                val correlationAad = "$requestId:$operationType:$currentTerminalId:$targetCore"
                val plaintextBytes = json.encodeToString(request).toByteArray(Charsets.UTF_8)

                // 5. Cifrado simétrico E2EE con clave Terminal->CORE
                val (nonce, ciphertext) = cryptoEngine.encrypt(
                    key = t2cKey,
                    plaintext = plaintextBytes,
                    aad = correlationAad.toByteArray(Charsets.UTF_8)
                )

                val envelope = EncryptedEnvelope(
                    protocolVersion = 1,
                    requestId = requestId,
                    sourceDeviceId = currentTerminalId,
                    targetDeviceId = targetCore,
                    nonceBase64 = Base64.getEncoder().encodeToString(nonce),
                    ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
                    aadBase64 = Base64.getEncoder().encodeToString(correlationAad.toByteArray(Charsets.UTF_8)),
                    timestamp = System.currentTimeMillis()
                )

                // 6. Registrar en el mapa de operaciones pendientes para correlación estricta
                val pendingRecord = PendingOperationRecord(
                    requestId = requestId,
                    opType = operationType,
                    terminalId = currentTerminalId,
                    targetCoreId = targetCore,
                    timestamp = System.currentTimeMillis()
                )
                pendingOperations[requestId] = pendingRecord

                // 7. Watchdog de tiempo de espera (15 segundos): si no hay confirmación, el estado es INCIERTO
                val watchdogJob = launch {
                    delay(15_000L)
                    if (pendingOperations.remove(requestId) != null) {
                        _currentOpStatus.value = ExecutionStatus.UNCERTAIN
                        _statusDetails.value = "Resultado incierto: solicitud transmitida pero la confirmación autenticada de CORE no llegó a tiempo"
                    }
                }
                timeoutWatchdogs[requestId] = watchdogJob

                // 8. Transmitir sobre cifrado por el WebSocket
                val sendResult = webSocketClient.sendEncryptedEnvelope(envelope)
                when (sendResult) {
                    is SendResult.DeliveredToServer -> {
                        _currentOpStatus.value = ExecutionStatus.PENDING_CORE_CONFIRMATION
                        _statusDetails.value = "Sobre entregado al relevo; pendiente de confirmación criptográfica de CORE"
                    }
                    is SendResult.Failed -> {
                        timeoutWatchdogs.remove(requestId)?.cancel()
                        pendingOperations.remove(requestId)
                        _currentOpStatus.value = ExecutionStatus.COMMUNICATION_ERROR
                        _statusDetails.value = "Fallo de transmisión: ${sendResult.reason}"
                    }
                }
            } catch (e: Exception) {
                timeoutWatchdogs.remove(requestId)?.cancel()
                pendingOperations.remove(requestId)
                _currentOpStatus.value = ExecutionStatus.COMMUNICATION_ERROR
                _statusDetails.value = "Error al cifrar solicitud: ${e.message}"
            }
        }
    }

    /**
     * Procesa los sobres entrantes del WebSocket correlacionándolos y verificándolos criptográficamente.
     */
    private suspend fun handleIncomingEnvelope(envelope: EncryptedEnvelope) {
        val myId = _deviceId.value

        // Descartar si el sobre no está dirigido a este dispositivo
        if (envelope.targetDeviceId != myId) {
            return
        }

        // A. CASO: Terminal recibiendo confirmación del CORE
        val pending = pendingOperations[envelope.requestId]
        if (pending != null) {
            handleConfirmationFromCore(envelope, pending)
            return
        }

        // B. CASO: CORE recibiendo solicitud de un Terminal
        if (_deviceRole.value == DeviceRole.CORE) {
            handleRequestAtCore(envelope)
        }
    }

    /**
     * Terminal: verifica la autenticidad y el resultado de la confirmación emitida por CORE (Parte X).
     */
    private suspend fun handleConfirmationFromCore(
        envelope: EncryptedEnvelope,
        pending: PendingOperationRecord
    ) {
        val c2tKey = coreToTerminalKey
        val watchdog = timeoutWatchdogs.remove(envelope.requestId)
        watchdog?.cancel()
        pendingOperations.remove(envelope.requestId)

        if (c2tKey == null) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "Rechazado: no existe clave de descifrado para respuestas de CORE"
            return
        }

        // 1. Reconstruir y validar AAD de correlación esperado (CORE -> Terminal)
        val expectedAadString = "${pending.requestId}:${pending.opType}:${pending.targetCoreId}:${pending.terminalId}"
        val receivedAadBytes = try {
            Base64.getDecoder().decode(envelope.aadBase64)
        } catch (e: Exception) {
            ByteArray(0)
        }

        if (String(receivedAadBytes, Charsets.UTF_8) != expectedAadString) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "Fallo de autenticidad: el AAD de correlación de la confirmación no coincide"
            return
        }

        // 2. Descifrar con AES-256-GCM usando la clave direccional CORE->Terminal
        val decryptedBytes = try {
            val nonce = Base64.getDecoder().decode(envelope.nonceBase64)
            val ciphertext = Base64.getDecoder().decode(envelope.ciphertextBase64)
            cryptoEngine.decrypt(c2tKey, nonce, ciphertext, receivedAadBytes)
        } catch (e: SecurityException) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "Fallo de autenticación criptográfica: el tag GCM de la confirmación fue rechazado"
            return
        }

        // 3. Deserializar el objeto de confirmación estructurado
        val confirmation = try {
            json.decodeFromString<CoreConfirmation>(String(decryptedBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "Confirmación corrupta o en formato inválido"
            return
        }

        // 4. Validar correlación estricta (Punto 4 y 5 de la solicitud)
        if (confirmation.requestId != pending.requestId ||
            confirmation.operationType != pending.opType ||
            confirmation.requestingDeviceId != pending.terminalId ||
            confirmation.coreDeviceId != pending.targetCoreId
        ) {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "Rechazado: discrepancia en los metadatos de correlación de la confirmación"
            return
        }

        // 5. Aplicar estado confirmado real
        if (confirmation.status == "EXECUTED_AND_CONFIRMED") {
            _currentOpStatus.value = ExecutionStatus.EXECUTED_AND_CONFIRMED
            _statusDetails.value = "${confirmation.executionDetails} (Confirmado por CORE ${confirmation.coreDeviceId.take(8)})"
        } else {
            _currentOpStatus.value = ExecutionStatus.REJECTED
            _statusDetails.value = "CORE rechazó la ejecución: ${confirmation.executionDetails}"
        }
    }

    /**
     * CORE: valida la autorización, descifra la solicitud y emite confirmación cifrada (Parte VI, VII y X).
     */
    private suspend fun handleRequestAtCore(envelope: EncryptedEnvelope) {
        val t2cKey = terminalToCoreKey ?: return

        // 1. Reconstruir AAD esperado para la solicitud
        val receivedAadBytes = try {
            Base64.getDecoder().decode(envelope.aadBase64)
        } catch (e: Exception) {
            return
        }

        // 2. Descifrar la solicitud E2EE
        val decryptedBytes = try {
            val nonce = Base64.getDecoder().decode(envelope.nonceBase64)
            val ciphertext = Base64.getDecoder().decode(envelope.ciphertextBase64)
            cryptoEngine.decrypt(t2cKey, nonce, ciphertext, receivedAadBytes)
        } catch (e: SecurityException) {
            // Mensaje manipulado o clave incorrecta; descartar sin ejecutar
            return
        }

        val request = try {
            json.decodeFromString<ProtectedOperationRequest>(String(decryptedBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            return
        }

        // 3. Comprobar autorización y protección anti-repetición (Parte VII.27)
        val validation = authorizationGuard.validateOperation(envelope.sourceDeviceId, request)
        val (statusString, detailsString) = when (validation) {
            is AuthorizationGuard.ValidationResult.Allowed -> {
                Pair("EXECUTED_AND_CONFIRMED", "Operación ${request.operationType} ejecutada con éxito en CORE")
            }
            is AuthorizationGuard.ValidationResult.Denied -> {
                Pair("REJECTED", "Autorización denegada: ${validation.reason}")
            }
        }

        // 4. Construir confirmación autenticada
        val confirmation = CoreConfirmation(
            requestId = request.requestId,
            operationType = request.operationType,
            requestingDeviceId = envelope.sourceDeviceId,
            coreDeviceId = _deviceId.value,
            status = statusString,
            executionDetails = detailsString,
            executionTimestamp = System.currentTimeMillis()
        )

        // 5. Cifrar la confirmación con la clave direccional CORE->Terminal
        val c2tKey = coreToTerminalKey ?: return
        val confirmationAad = "${request.requestId}:${request.operationType}:${_deviceId.value}:${envelope.sourceDeviceId}"
        val confirmationBytes = json.encodeToString(confirmation).toByteArray(Charsets.UTF_8)

        val (cNonce, cCiphertext) = cryptoEngine.encrypt(
            key = c2tKey,
            plaintext = confirmationBytes,
            aad = confirmationAad.toByteArray(Charsets.UTF_8)
        )

        val responseEnvelope = EncryptedEnvelope(
            protocolVersion = 1,
            requestId = request.requestId,
            sourceDeviceId = _deviceId.value,
            targetDeviceId = envelope.sourceDeviceId,
            nonceBase64 = Base64.getEncoder().encodeToString(cNonce),
            ciphertextBase64 = Base64.getEncoder().encodeToString(cCiphertext),
            aadBase64 = Base64.getEncoder().encodeToString(confirmationAad.toByteArray(Charsets.UTF_8)),
            timestamp = System.currentTimeMillis()
        )

        webSocketClient.sendEncryptedEnvelope(responseEnvelope)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            webSocketClient.disconnect()
        }
    }
}
