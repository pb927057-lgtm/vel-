package com.vel.server

import java.util.concurrent.ConcurrentHashMap

/**
 * Registro de Revocación y Control de Acceso en el Servidor de Relevo (Parte VIII.30).
 * Aplica el rechazo inmediato de conexiones y retransmisiones para terminales revocados por CORE.
 */
class RevocationRegistry {
    // Almacena terminalId -> timestamp de revocación
    private val revokedDevices = ConcurrentHashMap<String, Long>()

    fun recordRevocation(terminalId: String) {
        revokedDevices[terminalId] = System.currentTimeMillis()
    }

    fun isRevoked(terminalId: String): Boolean {
        return revokedDevices.containsKey(terminalId)
    }

    fun getRevokedDevicesCount(): Int = revokedDevices.size
}
