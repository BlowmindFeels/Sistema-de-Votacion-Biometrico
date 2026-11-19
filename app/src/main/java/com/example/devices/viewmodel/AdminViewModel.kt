package com.example.devices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devices.data.AdminRepository
import com.example.devices.data.ApiVotacionRepository
import com.example.devices.data.EstadoVotacion
import com.example.devices.data.SesionManager
import com.example.devices.data.Usuario
import com.example.devices.data.Votacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Acción que espera confirmación del administrador antes de ejecutarse. */
sealed interface AccionAdmin {
    data class CambiarEstado(val votacion: Votacion, val nuevo: EstadoVotacion) : AccionAdmin
    data class CambiarRol(val usuario: Usuario, val hacerAdmin: Boolean) : AccionAdmin
}

data class AdminUiState(
    val pestana: Int = 0,
    val cargandoVotaciones: Boolean = true,
    val votaciones: List<Votacion> = emptyList(),
    val cargandoUsuarios: Boolean = true,
    val usuarios: List<Usuario> = emptyList(),
    val documentoPropio: String? = null,
    val procesando: Boolean = false,
    val pendiente: AccionAdmin? = null,
    val error: String? = null,
    val mensaje: String? = null
)

class AdminViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState(documentoPropio = SesionManager.documento))
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    /** Se llama cada vez que la pantalla aparece (también al volver de crear una votación). */
    fun cargar() {
        cargarVotaciones()
        cargarUsuarios()
    }

    private fun cargarVotaciones() {
        viewModelScope.launch {
            _state.update { it.copy(cargandoVotaciones = true) }
            ApiVotacionRepository.listar()
                .onSuccess { lista ->
                    _state.update { s -> s.copy(cargandoVotaciones = false, votaciones = lista.sortedWith(ordenAdmin)) }
                }
                .onFailure { e -> _state.update { it.copy(cargandoVotaciones = false, error = e.message) } }
        }
    }

    private fun cargarUsuarios() {
        viewModelScope.launch {
            _state.update { it.copy(cargandoUsuarios = true) }
            AdminRepository.usuarios()
                .onSuccess { lista -> _state.update { it.copy(cargandoUsuarios = false, usuarios = lista) } }
                .onFailure { e -> _state.update { it.copy(cargandoUsuarios = false, error = e.message) } }
        }
    }

    fun seleccionarPestana(i: Int) = _state.update { it.copy(pestana = i) }

    fun pedir(accion: AccionAdmin) = _state.update { it.copy(pendiente = accion) }

    fun cancelar() = _state.update { it.copy(pendiente = null) }

    fun confirmar() {
        val accion = _state.value.pendiente ?: return
        _state.update { it.copy(pendiente = null, procesando = true, error = null) }
        viewModelScope.launch {
            when (accion) {
                is AccionAdmin.CambiarEstado ->
                    AdminRepository.cambiarEstado(accion.votacion.id, accion.nuevo)
                        .onSuccess { v ->
                            // Se actualiza con lo que devolvió la primaria (la réplica puede ir atrás).
                            _state.update { s ->
                                s.copy(
                                    procesando = false,
                                    votaciones = s.votaciones.map { if (it.id == v.id) v else it }.sortedWith(ordenAdmin),
                                    mensaje = "«${v.titulo}» ahora está ${nombreEstado(v.estado)}"
                                )
                            }
                        }
                        .onFailure { e -> _state.update { it.copy(procesando = false, error = e.message) } }

                is AccionAdmin.CambiarRol ->
                    AdminRepository.cambiarRol(accion.usuario.id, accion.hacerAdmin)
                        .onSuccess { u ->
                            _state.update { s ->
                                s.copy(
                                    procesando = false,
                                    usuarios = s.usuarios.map { if (it.id == u.id) u else it },
                                    mensaje = if (u.esAdmin) "${u.nombre} ahora es administrador"
                                              else "${u.nombre} ya no es administrador"
                                )
                            }
                        }
                        .onFailure { e -> _state.update { it.copy(procesando = false, error = e.message) } }
            }
        }
    }

    fun errorMostrado() = _state.update { it.copy(error = null) }
    fun mensajeMostrado() = _state.update { it.copy(mensaje = null) }

    private companion object {
        // Primero lo que requiere atención: activas, luego programadas, luego cerradas.
        val ordenAdmin = compareBy<Votacion>(
            { when (it.estado) {
                EstadoVotacion.ACTIVA -> 0
                EstadoVotacion.PROGRAMADA -> 1
                EstadoVotacion.FINALIZADA -> 2
                EstadoVotacion.CANCELADA -> 3
            } },
            { -it.id }
        )
    }
}

fun nombreEstado(e: EstadoVotacion) = when (e) {
    EstadoVotacion.ACTIVA -> "activa"
    EstadoVotacion.PROGRAMADA -> "programada"
    EstadoVotacion.FINALIZADA -> "finalizada"
    EstadoVotacion.CANCELADA -> "cancelada"
}
