package com.example.devices.data

interface AuthRepository {
    suspend fun login(documento: String, password: String): Result<String>
    suspend fun registrar(nombre: String, documento: String, correo: String, password: String): Result<Unit>
    fun haySesion(): Boolean
    fun cerrarSesion()
}