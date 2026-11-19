package com.example.devices.data

/**
 * Sesión en memoria: el token JWT del Auth-Service y el nombre del usuario.
 * El token no se guarda en texto plano en disco; la copia persistente va
 * cifrada con el Keystore (ver util/TokenSeguro).
 */
object SesionManager {
    @Volatile var token: String? = null
        private set
    @Volatile var nombre: String? = null
        private set

    fun iniciar(token: String, nombre: String) {
        this.token = token
        this.nombre = nombre
    }

    fun cerrar() {
        token = null
        nombre = null
    }

    fun bearer(): String? = token?.let { "Bearer $it" }
}
