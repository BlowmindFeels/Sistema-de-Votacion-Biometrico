package com.example.devices.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/registro")
    suspend fun registro(@Body body: RegistroRequest): Response<OkResponse>

    // ---- Solo ADMIN ----
    @GET("auth/usuarios")
    suspend fun usuarios(@Header("Authorization") bearer: String): Response<List<UsuarioDto>>

    @PATCH("auth/usuarios/{id}/rol")
    suspend fun cambiarRol(
        @Path("id") id: Int,
        @Body body: RolRequest,
        @Header("Authorization") bearer: String
    ): Response<UsuarioDto>
}
