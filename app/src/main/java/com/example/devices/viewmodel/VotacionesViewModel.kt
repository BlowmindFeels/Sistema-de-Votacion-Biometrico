package com.example.devices.viewmodel

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

data class VotacionesUiState(
    val cargando: Boolean = true,
    val votaciones: List<Votacion> = emptyList(),
    val error: String? = null
)

class VotacionesViewModel : ViewModel() {

    private val repo: VotacionRepository = ApiVotacionRepository

    private val _state = MutableStateFlow(VotacionesUiState())
    val state: StateFlow<VotacionesUiState> = _state.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            repo.listar()
                .onSuccess { lista -> _state.update { it.copy(cargando = false, votaciones = lista) } }
                .onFailure { e -> _state.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}