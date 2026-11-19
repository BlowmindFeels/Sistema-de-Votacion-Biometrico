package com.example.devices.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

object Biometria {

    private const val AUTENTICADORES = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /** Devuelve null si se puede autenticar, o el mensaje que explica por qué no. */
    fun mensajeNoDisponible(context: Context): String? =
        when (BiometricManager.from(context).canAuthenticate(AUTENTICADORES)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Este dispositivo no tiene sensor de huella"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "El sensor de huella no está disponible en este momento"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No hay huellas registradas. Agrega una en Ajustes del teléfono"
            else -> "La autenticación biométrica no está disponible"
        }

    fun autenticar(
        activity: FragmentActivity,
        cipher: Cipher,
        onExito: (Cipher) -> Unit,
        onCancelado: () -> Unit,
        onError: (String) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // El cipher solo queda habilitado por el Keystore tras la huella.
                val c = result.cryptoObject?.cipher
                if (c != null) onExito(c) else onError("La huella no habilitó la clave segura")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> onCancelado()
                    else -> onError(errString.toString())
                }
            }
            // onAuthenticationFailed (huella no reconocida): el diálogo sigue abierto
            // y Android muestra el aviso solo. No hay que hacer nada.
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            callback
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirma tu identidad")
            .setSubtitle("Usa tu huella para emitir el voto")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(AUTENTICADORES)
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}

/** Compose entrega a veces un Context envuelto; esto encuentra la Activity real. */
fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}