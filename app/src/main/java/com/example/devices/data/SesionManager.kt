package com.example.devices.data

/**
 * Sesión en memoria: el token JWT del Auth-Service, el nombre, el documento y el rol.
 * El token no se guarda en texto plano en disco; la copia persistente va
 * cifrada con el Keystore (ver util/TokenSeguro).
 *
 * El rol solo decide qué pantallas se muestran: quien autoriza de verdad es el
 * backend, que revisa el rol dentro del token firmado en cada petición.
 */
object SesionManager {
    @Volatile var token: String? = null
        private set
    @Volatile var nombre: String? = null
        private set
    @Volatile var documento: String? = null
        private set
    @Volatile var rol: String? = null
        private set

    val esAdmin: Boolean get() = rol == "ADMIN"

    fun iniciar(token: String, nombre: String, documento: String, rol: String?) {
        this.token = token
        this.nombre = nombre
        this.documento = documento
        this.rol = rol ?: "CIUDADANO"
    }

    fun cerrar() {
        token = null
        nombre = null
        documento = null
        rol = null
    }

    fun bearer(): String? = token?.let { "Bearer $it" }
}
