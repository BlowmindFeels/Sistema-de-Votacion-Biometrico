package com.example.devices.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface VotingApi {
    @GET("votaciones")
    suspend fun listar(): Response<List<VotacionDto>>

    @GET("votaciones/{id}")
    suspend fun detalle(@Path("id") id: Int): Response<VotacionDto>

    @GET("votaciones/{id}/resultados")
    suspend fun resultados(@Path("id") id: Int): Response<ResultadosDto>

    @GET("votaciones/{id}/mi-voto")
    suspend fun miVoto(
        @Path("id") id: Int,
        @Header("Authorization") bearer: String
    ): Response<MiVotoResponse>

    @POST("votaciones/{id}/votos")
    suspend fun votar(
        @Path("id") id: Int,
        @Body body: VotoRequest,
        @Header("Authorization") bearer: String
    ): Response<OkResponse>

    // ---- Solo ADMIN ----
    @POST("votaciones")
    suspend fun crear(
        @Body body: CrearVotacionRequest,
        @Header("Authorization") bearer: String
    ): Response<VotacionDto>

    @PATCH("votaciones/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Int,
        @Body body: EstadoRequest,
        @Header("Authorization") bearer: String
    ): Response<VotacionDto>
}
