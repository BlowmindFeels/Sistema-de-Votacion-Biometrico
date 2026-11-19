package com.example.devices.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devices.data.ApiVotacionRepository
import com.example.devices.data.EstadoVotacion
import com.example.devices.data.SesionManager
import com.example.devices.data.Votacion
import com.example.devices.data.VotacionRepository
import com.example.devices.util.TokenSeguro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** null = todas. */
data class VotacionesUiState(
    val cargando: Boolean = true,
    val refrescando: Boolean = false,
    val votaciones: List<Votacion> = emptyList(),
    val filtro: EstadoVotacion? = null,
    val nombre: String = "",
    val esAdmin: Boolean = false,
    val error: String? = null
) {
    val visibles: List<Votacion>
        get() = if (filtro == null) votaciones else votaciones.filter { it.estado == filtro }

    fun cuenta(estado: EstadoVotacion?) =
        if (estado == null) votaciones.size else votaciones.count { it.estado == estado }
}

class VotacionesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: VotacionRepository = ApiVotacionRepository

    private val _state = MutableStateFlow(
        VotacionesUiState(
            nombre = SesionManager.nombre.orEmpty(),
            esAdmin = SesionManager.esAdmin
        )
    )
    val state: StateFlow<VotacionesUiState> = _state.asStateFlow()

    /** La pantalla lo llama al aparecer, así la lista se actualiza al volver de votar. */
    fun cargar(comoRefresco: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    cargando = !comoRefresco && it.votaciones.isEmpty(),
                    refrescando = comoRefresco,
                    error = null
                )
            }
            repo.listar()
                .onSuccess { lista ->
                    _state.update {
                        it.copy(
                            cargando = false,
                            refrescando = false,
                            votaciones = lista.sortedWith(ordenCiudadano)
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(cargando = false, refrescando = false, error = e.message) }
                }
        }
    }

    fun filtrar(estado: EstadoVotacion?) = _state.update { it.copy(filtro = estado) }

    fun cerrarSesion() {
        SesionManager.cerrar()
        TokenSeguro.borrar(getApplication())
    }

    private companion object {
        val ordenCiudadano = compareBy<Votacion>(
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
