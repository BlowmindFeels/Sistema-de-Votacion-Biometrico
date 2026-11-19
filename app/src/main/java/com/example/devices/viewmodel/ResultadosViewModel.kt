package com.example.devices.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devices.data.ApiVotacionRepository
import com.example.devices.data.Resultados
import com.example.devices.data.VotacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultadosUiState(
    val cargando: Boolean = true,
    val titulo: String = "",
    val resultados: Resultados? = null,
    val error: String? = null
)

class ResultadosViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repo: VotacionRepository = ApiVotacionRepository
    private val votacionId: Int = checkNotNull(savedStateHandle.get<Int>("votacionId"))

    private val _state = MutableStateFlow(ResultadosUiState())
    val state: StateFlow<ResultadosUiState> = _state.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            val titulo = repo.obtener(votacionId).getOrNull()?.titulo.orEmpty()
            repo.resultados(votacionId)
                .onSuccess { r ->
                    _state.update { it.copy(cargando = false, titulo = titulo, resultados = r) }
                }
                .onFailure { e -> _state.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}
