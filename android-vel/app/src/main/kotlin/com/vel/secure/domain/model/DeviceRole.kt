package com.vel.secure.domain.model

enum class DeviceRole(val label: String, val description: String) {
    UNCONFIGURED("Sin Configurar", "Dispositivo sin rol de seguridad asignado"),
    CORE("VEL CORE", "Dispositivo custodio y autoridad de seguridad"),
    TERMINAL("VEL Terminal", "Dispositivo móvil para conexión segura con CORE")
}

enum class AuthorizationStatus(val label: String) {
    PENDING("Pendiente"),
    AUTHORIZED("Autorizado"),
    REVOKED("Revocado")
}

enum class ExecutionStatus(val label: String) {
    PREPARING("Preparando solicitud"),
    SENT("Enviada"),
    RECEIVED_BY_SERVER("Recibida por el servidor"),
    PENDING_CORE_CONFIRMATION("Pendiente de confirmación de CORE"),
    EXECUTED_AND_CONFIRMED("Ejecutada y confirmada"),
    REJECTED("Rechazada"),
    NOT_EXECUTED("No ejecutada"),
    UNCERTAIN("Resultado incierto"),
    COMMUNICATION_ERROR("Error de comunicación")
}
