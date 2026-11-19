package com.example.devices.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.devices.data.AuthRepository
import com.example.devices.data.ApiAuthRepository
import com.example.devices.util.TokenSeguro
import com.example.devices.util.Validadores

data class LoginUiState(
    val documento: String = "",
    val password: String = "",
    val errorDocumento: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    val loginExitoso: Boolean = false
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: AuthRepository = ApiAuthRepository

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onDocumentoChange(v: String) {
        if (v.length <= 10 && v.all { it.isDigit() }) {
            _state.update { it.copy(documento = v, errorDocumento = null, errorGeneral = null) }
        }
    }

    fun onPasswordChange(v: String) {
        _state.update { it.copy(password = v, errorPassword = null, errorGeneral = null) }
    }

    fun login() {
        val s = _state.value
        val errDoc = Validadores.documento(s.documento)
        val errPass = if (s.password.isBlank()) "Ingresa tu contraseña" else null

        if (errDoc != null || errPass != null) {
            _state.update { it.copy(errorDocumento = errDoc, errorPassword = errPass) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(cargando = true, errorGeneral = null) }
            repo.login(s.documento, s.password)
                .onSuccess { _ ->
                    // Nueva sesión: se descarta el token cifrado de la sesión anterior.
                    // El nuevo se cifra con el Keystore en la primera huella.
                    TokenSeguro.borrar(getApplication())
                    _state.update { it.copy(cargando = false, loginExitoso = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(cargando = false, errorGeneral = e.message) }
                }
        }
    }
}



