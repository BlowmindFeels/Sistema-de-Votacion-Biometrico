package com.example.devices.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

object FirebaseVotacionRepository : VotacionRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun votacionRef(id: Int) = db.collection("votaciones").document(id.toString())
    private fun votoRef(votacionId: Int, uid: String) =
        votacionRef(votacionId).collection("votos").document(uid)

    // Convierte un documento de Firestore en nuestro modelo Votacion
    private fun DocumentSnapshot.aVotacion(): Votacion? {
        val opciones = (get("opciones") as? List<*>).orEmpty().mapNotNull { o ->
            val m = o as? Map<*, *> ?: return@mapNotNull null
            val idOpcion = (m["id"] as? Number)?.toInt() ?: return@mapNotNull null
            Opcion(idOpcion, m["nombre"] as? String ?: "")
        }
        return Votacion(
            id = id.toIntOrNull() ?: return null,
            titulo = getString("titulo") ?: "",
            descripcion = getString("descripcion") ?: "",
            fechaInicio = getString("fechaInicio") ?: "",
            fechaFin = getString("fechaFin") ?: "",
            estado = runCatching { EstadoVotacion.valueOf(getString("estado") ?: "") }
                .getOrDefault(EstadoVotacion.PROGRAMADA),
            opciones = opciones
        )
    }

    override suspend fun listar(): Result<List<Votacion>> = try {
        val docs = db.collection("votaciones").get().await().documents
        Result.success(docs.mapNotNull { it.aVotacion() }.sortedBy { it.id })
    } catch (e: Exception) {
        Result.failure(Exception("No se pudieron cargar las votaciones: ${e.message}"))
    }

    override suspend fun obtener(id: Int): Result<Votacion> = try {
        val v = votacionRef(id).get().await().aVotacion()
        if (v != null) Result.success(v) else Result.failure(Exception("No se encontró la votación"))
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo cargar la votación: ${e.message}"))
    }

    override suspend fun yaVoto(votacionId: Int): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            votoRef(votacionId, uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun votar(votacionId: Int, opcionId: Int): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Tu sesión expiró, inicia sesión de nuevo"))

        val v = obtener(votacionId).getOrElse { return Result.failure(it) }
        if (v.estado != EstadoVotacion.ACTIVA)
            return Result.failure(Exception("Esta votación no está abierta"))
        if (v.opciones.none { it.id == opcionId })
            return Result.failure(Exception("Opción no válida"))
        if (yaVoto(votacionId))
            return Result.failure(Exception("Ya has votado en esta votación"))

        return try {
            votoRef(votacionId, uid).set(
                mapOf("opcionId" to opcionId, "fecha" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            // Las reglas rechazan sobrescribir un voto: si llega aquí, ya votó
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                Result.failure(Exception("Ya has votado en esta votación"))
            else Result.failure(Exception("No se pudo registrar el voto: ${e.message}"))
        }
    }

    // USO ÚNICO: sube las votaciones de prueba a Firestore. Bórrala después.
    suspend fun sembrarDatosDePrueba() {
        FakeVotacionRepository.listar().getOrThrow().forEach { v ->
            votacionRef(v.id).set(
                mapOf(
                    "titulo" to v.titulo,
                    "descripcion" to v.descripcion,
                    "fechaInicio" to v.fechaInicio,
                    "fechaFin" to v.fechaFin,
                    "estado" to v.estado.name,
                    "opciones" to v.opciones.map { mapOf("id" to it.id, "nombre" to it.nombre) }
                )
            ).await()
        }
    }
}