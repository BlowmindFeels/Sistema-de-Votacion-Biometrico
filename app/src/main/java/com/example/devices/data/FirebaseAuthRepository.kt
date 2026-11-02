package com.example.devices.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseAuthRepository : AuthRepository {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Firebase pide correo; el usuario solo escribe documento.
    private fun correoSintetico(documento: String) = "$documento@votacion.app"

    override suspend fun login(documento: String, password: String): Result<String> =
        try {
            val res = auth.signInWithEmailAndPassword(correoSintetico(documento), password).await()
            val token = res.user?.getIdToken(false)?.await()?.token ?: ""
            Result.success(token)
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Documento o contraseña incorrectos"))
        } catch (e: Exception) {
            // Firebase también devuelve "invalid credential" como excepción genérica de usuario
            val msg = if ((e.message?.contains("credential", ignoreCase = true) == true) ||
                (e.message?.contains("no user record", ignoreCase = true) == true))
                "Documento o contraseña incorrectos"
            else "No se pudo iniciar sesión: ${e.message}"
            Result.failure(Exception(msg))
        }

    override suspend fun registrar(
        nombre: String, documento: String, correo: String, password: String
    ): Result<Unit> =
        try {
            val res = auth.createUserWithEmailAndPassword(correoSintetico(documento), password).await()
            val uid = res.user?.uid ?: throw Exception("No se pudo crear el usuario")
            db.collection("usuarios").document(uid).set(
                mapOf(
                    "nombre" to nombre.trim(),
                    "documento" to documento,
                    "correo" to correo.trim()
                )
            ).await()
            auth.signOut() // tras registrarse, vuelve al login
            Result.success(Unit)
        } catch (_: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este documento ya está registrado"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo registrar: ${e.message}"))
        }

    override fun haySesion(): Boolean = auth.currentUser != null

    override fun cerrarSesion() = auth.signOut()
}
