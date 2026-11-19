package com.example.devices.data.remote

/**
 * Dirección del API Gateway publicado por Docker para el emulador Android.
 * En un teléfono físico, usa la IP LAN del PC y permite el puerto en el firewall.
 */
object ApiConfig {
    const val AUTH_BASE_URL = "http://192.168.1.8:8081/"
    const val VOTING_BASE_URL = "http://192.168.1.8:8081/"
}
