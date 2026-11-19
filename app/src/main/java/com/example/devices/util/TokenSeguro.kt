package com.example.devices.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Enlaza la huella con el token JWT usando el Android Keystore.
 *
 * - La clave AES vive en el hardware seguro y exige autenticación biométrica
 *   en CADA uso (setUserAuthenticationRequired, sin tiempo de validez).
 * - El token se guarda cifrado en SharedPreferences; sin huella no se puede
 *   descifrar, así que el voto solo puede firmarse con el token tras la huella.
 * - Si el usuario agrega una huella nueva en el teléfono, la clave se invalida
 *   y se crea otra (setInvalidatedByBiometricEnrollment).
 */
object TokenSeguro {

    private const val ALIAS = "votacion_token_key"
    private const val PREFS = "token_seguro"
    private const val K_TOKEN = "token_cifrado"
    private const val K_IV = "iv"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"

    /** Cipher listo para el BiometricPrompt y si se usará para cifrar o descifrar. */
    class Preparado(val cipher: Cipher, val cifrar: Boolean)

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun obtenerClave(): SecretKey {
        (keyStore().getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                }
            }
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    fun borrar(ctx: Context) {
        prefs(ctx).edit().remove(K_TOKEN).remove(K_IV).apply()
    }

    /**
     * Prepara el cipher. Si ya hay un token cifrado se descifra; si no, se cifrará
     * el token de la sesión actual. Devuelve null si el Keystore no está disponible.
     */
    fun preparar(ctx: Context): Preparado? = try {
        val p = prefs(ctx)
        val iv = p.getString(K_IV, null)
        val hayToken = p.getString(K_TOKEN, null) != null
        val cipher = Cipher.getInstance(TRANSFORMACION)
        try {
            if (iv != null && hayToken) {
                cipher.init(
                    Cipher.DECRYPT_MODE, obtenerClave(),
                    GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
                )
                Preparado(cipher, cifrar = false)
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, obtenerClave())
                Preparado(cipher, cifrar = true)
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Cambiaron las huellas del teléfono: se descarta la clave y el token viejo.
            keyStore().deleteEntry(ALIAS)
            borrar(ctx)
            cipher.init(Cipher.ENCRYPT_MODE, obtenerClave())
            Preparado(cipher, cifrar = true)
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Se llama con el cipher que devolvió el BiometricPrompt (ya autenticado).
     * Devuelve el token que se debe usar para votar.
     */
    fun usar(ctx: Context, preparado: Preparado, tokenSesion: String?): Result<String> = try {
        val cipher = preparado.cipher
        if (preparado.cifrar) {
            val token = tokenSesion ?: throw IllegalStateException("No hay sesión activa")
            val cifrado = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            prefs(ctx).edit()
                .putString(K_TOKEN, Base64.encodeToString(cifrado, Base64.NO_WRAP))
                .putString(K_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
            Result.success(token)
        } else {
            val cifrado = Base64.decode(prefs(ctx).getString(K_TOKEN, null), Base64.NO_WRAP)
            Result.success(String(cipher.doFinal(cifrado), Charsets.UTF_8))
        }
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo validar el token con la huella. Ingresa de nuevo."))
    }
}
