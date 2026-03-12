package com.example.pkmforms.api

import android.content.Context

object ApiConfig {

    private const val PREFS_NAME  = "pkm_config"
    private const val KEY_IP      = "server_ip"
    private const val IP_DEFAULT  = "192.168.1.25"
    private const val PORT        = "8080"

    // Obtiene la IP guardada en SharedPreferences
    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ip    = prefs.getString(KEY_IP, IP_DEFAULT) ?: IP_DEFAULT
        return "http://$ip:$PORT"
    }

    // Guarda una nueva IP en SharedPreferences
    fun guardarIp(context: Context, ip: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IP, ip.trim())
            .apply()
    }

    // Lee solo la IP guardada (sin http ni puerto)
    fun getIp(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IP, IP_DEFAULT) ?: IP_DEFAULT
    }

    fun endpointSubir(context: Context)      = "${getBaseUrl(context)}/formularios/subir"
    fun endpointLista(context: Context)      = "${getBaseUrl(context)}/formularios/lista"
    fun endpointDescargar(context: Context)  = "${getBaseUrl(context)}/formularios/descargar"
    fun endpointEliminar(context: Context)   = "${getBaseUrl(context)}/formularios/eliminar"
}