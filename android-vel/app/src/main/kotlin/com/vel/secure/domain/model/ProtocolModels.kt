package com.vel.secure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PairingQrPayload(
    val coreId: String,
    val oneTimeToken: String,
    val coreEphemeralPublicKeyHex: String,
    val expiresAt: Long
)

@Serializable
data class EncryptedEnvelope(
    val protocolVersion: Int = 1,
    val requestId: String,
    val sourceDeviceId: String,
    val targetDeviceId: String,
    val nonceBase64: String,
    val ciphertextBase64: String,
    val aadBase64: String,
    val timestamp: Long
)

@Serializable
data class ProtectedOperationRequest(
    val requestId: String,
    val operationType: String, // e.g., "LOCK_DEVICE", "SHIELD_DATA", "EMERGENCY_WIPE", "VERIFY_INTEGRITY"
    val parametersJson: String = "{}",
    val authToken: String,
    val clientTimestamp: Long
)

@Serializable
data class CoreConfirmation(
    val requestId: String,
    val operationType: String,
    val requestingDeviceId: String,
    val coreDeviceId: String,
    val status: String, // EXECUTED_AND_CONFIRMED, REJECTED, NOT_EXECUTED
    val executionDetails: String,
    val executionTimestamp: Long
)

@Serializable
data class RevocationInstruction(
    val instructionId: String,
    val coreDeviceId: String,
    val targetTerminalId: String,
    val reason: String,
    val timestamp: Long
)
