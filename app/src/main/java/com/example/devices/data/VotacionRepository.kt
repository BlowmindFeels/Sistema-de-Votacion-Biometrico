package com.example.devices.data


import kotlinx.coroutines.delay

interface VotacionRepository {
    suspend fun listar(): Result<List<Votacion>>
    suspend fun obtener(id: Int): Result<Votacion>
    suspend fun yaVoto(votacionId: Int): Boolean
    /** [token]: el JWT que se liberó del Keystore con la huella. */
    suspend fun votar(votacionId: Int, opcionId: Int, token: String): Result<Unit>
    suspend fun resultados(votacionId: Int): Result<Resultados>
}

object FakeVotacionRepository : VotacionRepository {

    private val votaciones = listOf(
        Votacion(
            id = 1,
            titulo = "Cambio de empresa de vigilancia",
            descripcion = "Se decide si se mantiene la empresa actual o se contrata una nueva para el conjunto residencial.",
            fechaInicio = "05/10/2026",
            fechaFin = "20/10/2026",
            estado = EstadoVotacion.ACTIVA,
            opciones = listOf(
                Opcion(1, "Mantener la empresa actual"),
                Opcion(2, "Contratar SegurAndes"),
                Opcion(3, "Contratar VigiNorte")
            )
        ),
        Votacion(
            id = 2,
            titulo = "Remodelación del salón comunal",
            descripcion = "¿Se aprueba el presupuesto para remodelar el salón comunal?",
            fechaInicio = "01/10/2026",
            fechaFin = "18/10/2026",
            estado = EstadoVotacion.ACTIVA,
            opciones = listOf(
                Opcion(4, "A favor"),
                Opcion(5, "En contra"),
                Opcion(6, "Abstención")
            )
        ),
        Votacion(
            id = 3,
            titulo = "Horario de la piscina",
            descripcion = "Definir el horario de uso de la piscina durante el fin de semana.",
            fechaInicio = "01/09/2026",
            fechaFin = "15/09/2026",
            estado = EstadoVotacion.FINALIZADA,
            opciones = listOf(
                Opcion(7, "8:00 a.m. a 6:00 p.m."),
                Opcion(8, "10:00 a.m. a 8:00 p.m.")
            )
        ),
        Votacion(
            id = 4,
            titulo = "Jornada de reciclaje",
            descripcion = "Elegir el día de la primera jornada de reciclaje del conjunto.",
            fechaInicio = "25/10/2026",
            fechaFin = "30/10/2026",
            estado = EstadoVotacion.PROGRAMADA,
            opciones = listOf(
                Opcion(9, "Sábado"),
                Opcion(10, "Domingo")
            )
        )
    )

    private val conteo = mutableMapOf<Int, Int>() // opcionId -> votos

    override suspend fun resultados(votacionId: Int): Result<Resultados> {
        delay(300)
        val v = votaciones.find { it.id == votacionId }
            ?: return Result.failure(Exception("No se encontró la votación"))
        val ops = v.opciones.map { OpcionResultado(it.id, it.nombre, conteo[it.id] ?: 0) }
        return Result.success(Resultados(votacionId, ops.sumOf { it.votos }, ops))
    }

    // Ids de votaciones en las que el usuario ya votó. Se pierde al cerrar la app.
    // Cuando exista el backend, esta regla la valida Voting-Service.
    private val votados = mutableSetOf<Int>()

    override suspend fun listar(): Result<List<Votacion>> {
        delay(600)
        return Result.success(votaciones)
    }

    override suspend fun obtener(id: Int): Result<Votacion> {
        delay(300)
        val v = votaciones.find { it.id == id }
        return if (v != null) Result.success(v)
        else Result.failure(Exception("No se encontró la votación"))
    }

    override suspend fun yaVoto(votacionId: Int): Boolean = votacionId in votados

    override suspend fun votar(votacionId: Int, opcionId: Int, token: String): Result<Unit> {
        delay(800)
        val v = votaciones.find { it.id == votacionId }
            ?: return Result.failure(Exception("No se encontró la votación"))

        if (v.estado != EstadoVotacion.ACTIVA)
            return Result.failure(Exception("Esta votación no está abierta"))
        if (v.opciones.none { it.id == opcionId })
            return Result.failure(Exception("Opción no válida"))
        if (votacionId in votados)
            return Result.failure(Exception("Ya has votado en esta votación"))

        votados.add(votacionId)
        conteo[opcionId] = (conteo[opcionId] ?: 0) + 1
        return Result.success(Unit)
    }
}