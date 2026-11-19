package com.example.devices.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.devices.data.ApiVotacionRepository
import com.example.devices.data.Votacion
import com.example.devices.data.VotacionRepository

data class DetalleUiState(
    val cargando: Boolean = true,
    val votacion: Votacion? = null,
    val opcionSeleccionada: Int? = null,
    val yaVoto: Boolean = false,
    val mostrarConfirmacion: Boolean = false,
    val pedirHuella: Boolean = false,
    val enviando: Boolean = false,
    val error: String? = null
)


class DetalleVotacionViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repo: VotacionRepository = ApiVotacionRepository
    private val votacionId: Int = checkNotNull(savedStateHandle.get<Int>("votacionId"))

    private val _state = MutableStateFlow(DetalleUiState())
    val state: StateFlow<DetalleUiState> = _state.asStateFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            repo.obtener(votacionId)
                .onSuccess { v ->
                    val yaVoto = repo.yaVoto(votacionId)
                    _state.update { it.copy(cargando = false, votacion = v, yaVoto = yaVoto) }
                }
                .onFailure { e -> _state.update { it.copy(cargando = false, error = e.message) } }
        }
    }

    fun seleccionarOpcion(id: Int) =
        _state.update { it.copy(opcionSeleccionada = id, error = null) }

    fun pedirConfirmacion() {
        if (_state.value.opcionSeleccionada != null) {
            _state.update { it.copy(mostrarConfirmacion = true) }
        }
    }

    fun cancelarConfirmacion() =
        _state.update { it.copy(mostrarConfirmacion = false) }

    // 1. El usuario pulsó "Votar" en el diálogo: ya no vota, solo pide la huella.
    fun confirmarVoto() {
        _state.update { it.copy(mostrarConfirmacion = false, pedirHuella = true, error = null) }
    }

    // 2. La huella salió bien y liberó el token del Keystore: ahora sí se vota.
    fun huellaExitosa(token: String) {
        val opcionId = _state.value.opcionSeleccionada ?: return
        viewModelScope.launch {
            _state.update { it.copy(pedirHuella = false, enviando = true) }
            repo.votar(votacionId, opcionId, token)
                .onSuccess { _state.update { it.copy(enviando = false, yaVoto = true) } }
                .onFailure { e -> _state.update { it.copy(enviando = false, error = e.message) } }
        }
    }

    // 3. La huella se canceló o falló: no se vota.
    fun huellaFallida(mensaje: String?) {
        _state.update { it.copy(pedirHuella = false, error = mensaje) }
    }
}