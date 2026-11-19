package com.example.devices.data

import com.example.devices.data.remote.ApiClient
import com.example.devices.data.remote.LoginRequest
import com.example.devices.data.remote.RegistroRequest
import com.example.devices.data.remote.llamar

/** Auth contra el Auth-Service (Node.js + bcrypt + JWT). */
object ApiAuthRepository : AuthRepository {

    override suspend fun login(documento: String, password: String): Result<String> =
        llamar { ApiClient.auth.login(LoginRequest(documento, password)) }
            .map { r ->
                SesionManager.iniciar(r.token, r.nombre, documento, r.rol)
                r.token
            }

    override suspend fun registrar(
        nombre: String, documento: String, correo: String, password: String
    ): Result<Unit> =
        llamar {
            ApiClient.auth.registro(
                RegistroRequest(nombre.trim(), documento, correo.trim(), password)
            )
        }.map { }
}
