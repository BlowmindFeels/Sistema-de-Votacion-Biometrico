package com.example.devices.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/registro")
    suspend fun registro(@Body body: RegistroRequest): Response<OkResponse>
}
