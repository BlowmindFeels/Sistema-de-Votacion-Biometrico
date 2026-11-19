package com.example.devices.data.remote

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val auth: AuthApi by lazy { retrofit(ApiConfig.AUTH_BASE_URL).create(AuthApi::class.java) }
    val voting: VotingApi by lazy { retrofit(ApiConfig.VOTING_BASE_URL).create(VotingApi::class.java) }
}

/** Error HTTP con el código, para que la UI distinga 401 / 409. */
class ApiException(val codigo: Int, mensaje: String) : Exception(mensaje)

/** Lee {"error": "..."} (Node) o {"detail": "..."} (FastAPI). */
internal fun mensajeDeError(r: Response<*>): String {
    val cuerpo = try { r.errorBody()?.string().orEmpty() } catch (_: Exception) { "" }
    val msg = try {
        val o = JsonParser.parseString(cuerpo).asJsonObject
        (o.get("error") ?: o.get("detail"))?.asString
    } catch (_: Exception) { null }
    return msg ?: "Error del servidor (${r.code()})"
}

/** Ejecuta una llamada y la convierte en Result, con mensajes en español. */
internal suspend fun <T> llamar(bloque: suspend () -> Response<T>): Result<T> =
    try {
        val r = bloque()
        val body = r.body()
        if (r.isSuccessful && body != null) Result.success(body)
        else Result.failure(ApiException(r.code(), mensajeDeError(r)))
    } catch (_: IOException) {
        Result.failure(Exception("No se pudo conectar con el servidor. ¿Está corriendo Docker?"))
    } catch (e: Exception) {
        Result.failure(Exception("Error inesperado: ${e.message}"))
    }
