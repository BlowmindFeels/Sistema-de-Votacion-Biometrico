package com.example.devices.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val VOTACIONES = "votaciones"

    const val DETALLE = "detalle/{votacionId}"
    fun detalle(id: Int) = "detalle/$id"

    const val RESULTADOS = "resultados/{votacionId}"
    fun resultados(id: Int) = "resultados/$id"

    // Solo para administradores
    const val ADMIN = "admin"
    const val NUEVA_VOTACION = "admin/nueva"
}
