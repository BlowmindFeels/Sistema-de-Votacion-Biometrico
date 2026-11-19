package com.example.devices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devices.data.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevaVotacionUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val duracionDias: Int = 7,
    val abrirAhora: Boolean = true,
    val opciones: List<String> = listOf("", ""),
    val errorTitulo: String? = null,
    val errorDescripcion: String? = null,
    val errorOpciones: String? = null,
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    val creada: Boolean = false
)

class NuevaVotacionViewModel : ViewModel() {

    private val _state = MutableStateFlow(NuevaVotacionUiState())
    val state: StateFlow<NuevaVotacionUiState> = _state.asStateFlow()

    fun onTitulo(v: String) {
        if (v.length <= 150) _state.update { it.copy(titulo = v, errorTitulo = null, errorGeneral = null) }
    }

    fun onDescripcion(v: String) {
        if (v.length <= 600) _state.update { it.copy(descripcion = v, errorDescripcion = null, errorGeneral = null) }
    }

    fun onDuracion(dias: Int) = _state.update { it.copy(duracionDias = dias.coerceIn(1, 90)) }

    fun onAbrirAhora(v: Boolean) = _state.update { it.copy(abrirAhora = v) }

    fun onOpcion(indice: Int, v: String) {
        if (v.length > 150) return
        _state.update { s ->
            s.copy(
                opciones = s.opciones.mapIndexed { i, o -> if (i == indice) v else o },
                errorOpciones = null,
                errorGeneral = null
            )
        }
    }

    fun agregarOpcion() = _state.update { s ->
        if (s.opciones.size >= MAX_OPCIONES) s else s.copy(opciones = s.opciones + "")
    }

    fun quitarOpcion(indice: Int) = _state.update { s ->
        if (s.opciones.size <= 2) s
        else s.copy(opciones = s.opciones.filterIndexed { i, _ -> i != indice }, errorOpciones = null)
    }

    fun guardar() {
        val s = _state.value
        val opciones = s.opciones.map { it.trim() }.filter { it.isNotEmpty() }

        val errTitulo = if (s.titulo.trim().length < 3) "Escribe un título de al menos 3 caracteres" else null
        val errDesc = if (s.descripcion.trim().length < 3) "Explica brevemente qué se decide" else null
        val errOpciones = when {
            opciones.size < 2 -> "Agrega al menos dos opciones"
            opciones.map { it.lowercase() }.toSet().size != opciones.size -> "Hay opciones repetidas"
            else -> null
        }
        if (errTitulo != null || errDesc != null || errOpciones != null) {
            _state.update {
                it.copy(errorTitulo = errTitulo, errorDescripcion = errDesc, errorOpciones = errOpciones)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(guardando = true, errorGeneral = null) }
            AdminRepository.crearVotacion(
                titulo = s.titulo.trim(),
                descripcion = s.descripcion.trim(),
                duracionDias = s.duracionDias,
                abrirAhora = s.abrirAhora,
                opciones = opciones
            )
                .onSuccess { _state.update { it.copy(guardando = false, creada = true) } }
                .onFailure { e -> _state.update { it.copy(guardando = false, errorGeneral = e.message) } }
        }
    }

    companion object {
        const val MAX_OPCIONES = 10
    }
}
