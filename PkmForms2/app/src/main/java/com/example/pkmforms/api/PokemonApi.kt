package com.example.pkmforms.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PokemonApi {

    /**
     * Obtiene los nombres de pokemon en el rango [min, max] inclusive.
     * Corre en Dispatchers.IO para no bloquear el hilo principal.
     */
    suspend fun obtenerPokemones(min: Int, max: Int): List<String> =
        withContext(Dispatchers.IO) {
            val resultado = mutableListOf<String>()
            for (id in min..max) {
                try {
                    val nombre = obtenerNombre(id)
                    if (nombre != null) resultado.add(nombre)
                } catch (e: Exception) {
                    android.util.Log.w("PokemonApi", "Error al obtener pokemon $id: ${e.message}")
                }
            }
            android.util.Log.d("PokemonApi", "Obtenidos ${resultado.size} pokemones ($min..$max): $resultado")
            resultado
        }

    private fun obtenerNombre(id: Int): String? {
        val url  = URL("https://pokeapi.co/api/v2/pokemon/$id")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout    = 8000
        conn.requestMethod  = "GET"
        return try {
            if (conn.responseCode == 200) {
                val texto = conn.inputStream.bufferedReader().readText()
                val json  = JSONObject(texto)
                json.getString("name").replaceFirstChar { it.uppercase() }
            } else {
                android.util.Log.w("PokemonApi", "HTTP ${conn.responseCode} para pokemon $id")
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}