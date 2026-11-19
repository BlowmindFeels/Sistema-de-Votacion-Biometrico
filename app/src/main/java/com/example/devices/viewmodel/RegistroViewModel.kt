package com.example.devices.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.devices.data.AuthRepository
import com.example.devices.util.Validadores
import com.example.devices.data.ApiAuthRepository

data class RegistroUiState(
    val nombre: String = "",
    val documento: String = "",
    val correo: String = "",
    val password: String = "",
    val errorNombre: String? = null,
    val errorDocumento: String? = null,
    val errorCorreo: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    val registroExitoso: Boolean = false
)

class RegistroViewModel : ViewModel() {

    private val repo: AuthRepository = ApiAuthRepository

    private val _state = MutableStateFlow(RegistroUiState())
    val state: StateFlow<RegistroUiState> = _state.asStateFlow()

    fun onNombreChange(v: String) =
        _state.update { it.copy(nombre = v, errorNombre = null, errorGeneral = null) }

    fun onDocumentoChange(v: String) {
        if (v.length <= 10 && v.all { it.isDigit() }) {
            _state.update { it.copy(documento = v, errorDocumento = null, errorGeneral = null) }
        }
    }

    fun onCorreoChange(v: String) =
        _state.update { it.copy(correo = v, errorCorreo = null, errorGeneral = null) }

    fun onPasswordChange(v: String) =
        _state.update { it.copy(password = v, errorPassword = null, errorGeneral = null) }

    fun registrar() {
        val s = _state.value
        val errNombre = Validadores.nombre(s.nombre)
        val errDoc = Validadores.documento(s.documento)
        val errCorreo = Validadores.correo(s.correo)
        val errPass = Validadores.password(s.password)

        if (listOf(errNombre, errDoc, errCorreo, errPass).any { it != null }) {
            _state.update {
                it.copy(
                    errorNombre = errNombre,
                    errorDocumento = errDoc,
                    errorCorreo = errCorreo,
                    errorPassword = errPass
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(cargando = true, errorGeneral = null) }
            repo.registrar(s.nombre, s.documento, s.correo, s.password)
                .onSuccess { _state.update { it.copy(cargando = false, registroExitoso = true) } }
                .onFailure { e -> _state.update { it.copy(cargando = false, errorGeneral = e.message) } }
        }
    }
}