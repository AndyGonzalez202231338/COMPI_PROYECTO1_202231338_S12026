package com.example.pkmforms.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ApiService {

    // Subir archivo .pkm al servidor
    suspend fun subirPkm(
        context: Context,
        archivo: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url        = URL(ApiConfig.endpointSubir(context))
            val boundary   = "----PkmFormsBoundary${System.currentTimeMillis()}"
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput      = true
                doInput       = true
                connectTimeout = 10_000
                readTimeout    = 15_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            val contenidoArchivo = archivo.readBytes()
            val nombre           = archivo.name

            connection.outputStream.use { os ->
                val inicio = "--$boundary\r\n" +
                        "Content-Disposition: form-data; name=\"archivo\"; filename=\"$nombre\"\r\n" +
                        "Content-Type: application/octet-stream\r\n\r\n"
                os.write(inicio.toByteArray())
                os.write(contenidoArchivo)
                os.write("\r\n--$boundary--\r\n".toByteArray())
                os.flush()
            }

            val codigo = connection.responseCode
            val cuerpo = if (codigo == 200)
                connection.inputStream.bufferedReader().readText()
            else
                connection.errorStream?.bufferedReader()?.readText() ?: "Error desconocido"
            connection.disconnect()

            if (codigo == 200) {
                val json = JSONObject(cuerpo)
                Result.success(json.optString("nombre", nombre))
            } else {
                val json = JSONObject(cuerpo)
                Result.failure(Exception(json.optString("error", "Error del servidor")))
            }

        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("No se pudo conectar. Verifica que el servidor este corriendo en ${ApiConfig.getBaseUrl(context)}"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("El servidor no respondio a tiempo"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    // LISTAR formularios disponibles en el servidor
    suspend fun listarFormularios(context: Context): Result<List<FormularioInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val url        = URL(ApiConfig.endpointLista(context))
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod  = "GET"
                    connectTimeout = 10_000
                    readTimeout    = 10_000
                }

                val codigo = connection.responseCode
                val cuerpo = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                if (codigo == 200) {
                    val json      = JSONObject(cuerpo)
                    val lista     = json.getJSONArray("formularios")
                    val resultado = mutableListOf<FormularioInfo>()
                    for (i in 0 until lista.length()) {
                        val item = lista.getJSONObject(i)
                        resultado.add(
                            FormularioInfo(
                                nombre = item.getString("nombre"),
                                tamano = item.getString("tamano"),
                                fecha  = item.getString("fecha")
                            )
                        )
                    }
                    Result.success(resultado)
                } else {
                    Result.failure(Exception("Error del servidor: $codigo"))
                }

            } catch (e: java.net.ConnectException) {
                Result.failure(Exception("No se pudo conectar al servidor"))
            } catch (e: Exception) {
                Result.failure(Exception("Error: ${e.message}"))
            }
        }
    // DESCARGAR un .pkm del servidor
    suspend fun descargarPkm(
        context: Context,
        nombre: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url        = URL("${ApiConfig.endpointDescargar(context)}/$nombre")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod  = "GET"
                connectTimeout = 10_000
                readTimeout    = 15_000
            }

            val codigo = connection.responseCode
            if (codigo == 200) {
                val bytes = connection.inputStream.readBytes()
                connection.disconnect()
                Result.success(bytes)
            } else {
                connection.disconnect()
                Result.failure(Exception("Archivo no encontrado en el servidor"))
            }

        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("No se pudo conectar al servidor"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message}"))
        }
    }
}

data class FormularioInfo(
    val nombre: String,
    val tamano: String,
    val fecha:  String
)