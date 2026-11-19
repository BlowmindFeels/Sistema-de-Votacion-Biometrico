package com.example.devices.data

import com.example.devices.data.remote.ApiClient
import com.example.devices.data.remote.ApiException
import com.example.devices.data.remote.CrearVotacionRequest
import com.example.devices.data.remote.EstadoRequest
import com.example.devices.data.remote.RolRequest
import com.example.devices.data.remote.UsuarioDto
import com.example.devices.data.remote.llamar

/**
 * Operaciones de administración. El backend vuelve a comprobar el rol ADMIN
 * del token en cada llamada: ocultar botones en la app no es la seguridad.
 */
object AdminRepository {

    private fun sinSesion(): Result<Nothing> =
        Result.failure(Exception("Tu sesión terminó. Ingresa de nuevo."))

    /** Traduce 401/403 a mensajes claros para el administrador. */
    private fun <T> Result<T>.conMensajes(): Result<T> = recoverCatching { e ->
        if (e is ApiException) when (e.codigo) {
            401 -> throw Exception("Tu sesión venció. Cierra sesión y vuelve a ingresar.")
            403 -> throw Exception("Tu cuenta no tiene permisos de administrador.")
        }
        throw e
    }

    suspend fun crearVotacion(
        titulo: String,
        descripcion: String,
        duracionDias: Int,
        abrirAhora: Boolean,
        opciones: List<String>
    ): Result<Votacion> {
        val bearer = SesionManager.bearer() ?: return sinSesion()
        val cuerpo = CrearVotacionRequest(titulo, descripcion, duracionDias, abrirAhora, opciones)
        return llamar { ApiClient.voting.crear(cuerpo, bearer) }
            .map { it.aModelo() }
            .conMensajes()
    }

    suspend fun cambiarEstado(votacionId: Int, estado: EstadoVotacion): Result<Votacion> {
        val bearer = SesionManager.bearer() ?: return sinSesion()
        return llamar { ApiClient.voting.cambiarEstado(votacionId, EstadoRequest(estado.name), bearer) }
            .map { it.aModelo() }
            .conMensajes()
    }

    suspend fun usuarios(): Result<List<Usuario>> {
        val bearer = SesionManager.bearer() ?: return sinSesion()
        return llamar { ApiClient.auth.usuarios(bearer) }
            .map { lista -> lista.map { it.aModelo() } }
            .conMensajes()
    }

    suspend fun cambiarRol(usuarioId: Int, hacerAdmin: Boolean): Result<Usuario> {
        val bearer = SesionManager.bearer() ?: return sinSesion()
        val rol = if (hacerAdmin) "ADMIN" else "CIUDADANO"
        return llamar { ApiClient.auth.cambiarRol(usuarioId, RolRequest(rol), bearer) }
            .map { it.aModelo() }
            .conMensajes()
    }
}

private fun UsuarioDto.aModelo() = Usuario(
    id = id,
    nombre = nombre,
    documento = documento,
    correo = correo,
    esAdmin = rol == "ADMIN"
)
