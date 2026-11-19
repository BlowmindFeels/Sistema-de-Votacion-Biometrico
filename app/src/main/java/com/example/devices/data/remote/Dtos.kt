package com.example.devices.data.remote

// ---- Auth-Service ----
data class LoginRequest(val documento: String, val password: String)
/** [rol]: "CIUDADANO" o "ADMIN" (null si el servidor es una versión anterior). */
data class LoginResponse(val token: String, val nombre: String, val rol: String?)
data class RegistroRequest(
    val nombre: String,
    val documento: String,
    val correo: String,
    val password: String
)
data class OkResponse(val ok: Boolean)

data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val documento: String,
    val correo: String,
    val rol: String
)
data class RolRequest(val rol: String)

// ---- Voting-Service ----
data class OpcionDto(val id: Int, val nombre: String)

data class VotacionDto(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val opciones: List<OpcionDto>
)

data class MiVotoResponse(val yaVoto: Boolean)
data class VotoRequest(val opcionId: Int)

data class OpcionResultadoDto(val id: Int, val nombre: String, val votos: Int)
data class ResultadosDto(
    val votacionId: Int,
    val total: Int,
    val opciones: List<OpcionResultadoDto>
)

// ---- Administración (Voting-Service) ----
data class CrearVotacionRequest(
    val titulo: String,
    val descripcion: String,
    val duracionDias: Int,
    val abrirAhora: Boolean,
    val opciones: List<String>
)
data class EstadoRequest(val estado: String)
