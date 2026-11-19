package com.example.devices.data

import com.example.devices.data.remote.ApiClient
import com.example.devices.data.remote.ApiException
import com.example.devices.data.remote.VotacionDto
import com.example.devices.data.remote.VotoRequest
import com.example.devices.data.remote.llamar

/** Votaciones contra el Voting-Service (FastAPI). Las lecturas salen de la réplica. */
object ApiVotacionRepository : VotacionRepository {

    override suspend fun listar(): Result<List<Votacion>> =
        llamar { ApiClient.voting.listar() }.map { lista -> lista.map { it.aModelo() } }

    override suspend fun obtener(id: Int): Result<Votacion> =
        llamar { ApiClient.voting.detalle(id) }.map { it.aModelo() }

    override suspend fun yaVoto(votacionId: Int): Boolean {
        val bearer = SesionManager.bearer() ?: return false
        return llamar { ApiClient.voting.miVoto(votacionId, bearer) }
            .getOrNull()?.yaVoto ?: false
    }

    override suspend fun votar(votacionId: Int, opcionId: Int, token: String): Result<Unit> =
        llamar { ApiClient.voting.votar(votacionId, VotoRequest(opcionId), "Bearer $token") }
            .map { }
            .recoverCatching { e ->
                if (e is ApiException && e.codigo == 401)
                    throw Exception("Tu sesión venció. Cierra sesión y vuelve a ingresar.")
                throw e
            }

    override suspend fun resultados(votacionId: Int): Result<Resultados> =
        llamar { ApiClient.voting.resultados(votacionId) }.map { r ->
            Resultados(
                votacionId = r.votacionId,
                total = r.total,
                opciones = r.opciones.map { OpcionResultado(it.id, it.nombre, it.votos) }
            )
        }
}

private fun VotacionDto.aModelo() = Votacion(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    fechaInicio = fechaCorta(fechaInicio),
    fechaFin = fechaCorta(fechaFin),
    estado = runCatching { EstadoVotacion.valueOf(estado) }.getOrDefault(EstadoVotacion.PROGRAMADA),
    opciones = opciones.map { Opcion(it.id, it.nombre) }
)

/** "2026-11-13T23:04:35.123+00:00" -> "13/11/2026". Sin java.time (minSdk 24). */
private fun fechaCorta(iso: String): String {
    val p = iso.take(10).split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
}
