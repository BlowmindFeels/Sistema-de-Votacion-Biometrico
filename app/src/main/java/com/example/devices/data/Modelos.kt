package com.example.devices.data

enum class EstadoVotacion { PROGRAMADA, ACTIVA, FINALIZADA, CANCELADA }

data class Opcion(
    val id: Int,
    val nombre: String
)

data class Votacion(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fechaInicio: String,   // dd/MM/yyyy (el repositorio convierte la fecha ISO del backend)
    val fechaFin: String,
    val estado: EstadoVotacion,
    val opciones: List<Opcion>
)

data class OpcionResultado(
    val id: Int,
    val nombre: String,
    val votos: Int
)

data class Resultados(
    val votacionId: Int,
    val total: Int,
    val opciones: List<OpcionResultado>
)
