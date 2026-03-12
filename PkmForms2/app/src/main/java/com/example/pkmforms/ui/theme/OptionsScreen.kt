package com.example.pkmforms.ui.theme

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.analyzer.PkmExporter
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.api.ApiConfig
import com.example.pkmforms.api.ApiService
import com.example.pkmforms.ui.theme.components.SaveFormDialog
import com.example.pkmforms.ui.theme.components.SavePkmDialog
import com.example.pkmforms.ui.theme.components.ServerConfigDialog
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun OptionsScreen(
    onBack: () -> Unit,
    onOpenCodeFile: () -> Unit,
    onSaveCodeFile: () -> Unit,
    onOpenFormFile: () -> Unit,
    onFinish: () -> Unit,
    formElements: List<FormElement> = emptyList(),
    codeText: String = "",
    hasErrors: Boolean = false,
    onCodeLoaded: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var mostrarDialogoPkm    by remember { mutableStateOf(false) }
    var mostrarDialogoForm   by remember { mutableStateOf(false) }
    var mostrarDialogoConfig by remember { mutableStateOf(false) }
    var subiendo             by remember { mutableStateOf(false) }
    var mensajeSubida        by remember { mutableStateOf<String?>(null) }

    // IP actual leida de SharedPreferences, se actualiza cuando el usuario la cambia
    var ipActual by remember { mutableStateOf(ApiConfig.getIp(context)) }

    val abrirFormLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val contenido = leerArchivoDesdeUri(context, uri)
            if (contenido != null) {
                onCodeLoaded(contenido)
                onBack()
                Toast.makeText(context, "Archivo cargado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text       = "Options",
            color      = AppColors.Text,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OptionButton(label = "Open code file") {
            abrirFormLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
        }
        Spacer(modifier = Modifier.height(12.dp))

        OptionButton(label = "Save code file") { mostrarDialogoForm = true }
        Spacer(modifier = Modifier.height(12.dp))

        OptionButton(label = "Open form file", onClick = onOpenFormFile)
        Spacer(modifier = Modifier.height(12.dp))

        OptionButton(
            label   = "Save form file",
            enabled = !hasErrors,
            onClick = { mostrarDialogoPkm = true }
        )
        if (hasErrors) {
            Text(
                text     = "Corrige los errores antes de guardar",
                color    = AppColors.Error,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Boton subir al servidor
        OptionButton(
            label   = if (subiendo) "Subiendo..." else "Upload to server",
            enabled = !hasErrors && !subiendo,
            onClick = {
                if (formElements.isEmpty()) {
                    Toast.makeText(context, "No hay formulario para subir", Toast.LENGTH_SHORT).show()
                    return@OptionButton
                }
                mensajeSubida = null
                mostrarDialogoPkm = true
            }
        )
        if (hasErrors) {
            Text(
                text     = "Corrige los errores antes de subir",
                color    = AppColors.Error,
                fontSize = 11.sp
            )
        }

        // Resultado de la subida
        mensajeSubida?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text     = msg,
                color    = if (msg.startsWith("Error")) AppColors.Error else AppColors.Accent,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Boton configurar servidor
        OptionButton(
            label   = "Server config",
            onClick = { mostrarDialogoConfig = true }
        )

        // Mostrar IP activa como referencia
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text     = "Servidor: http://$ipActual:8080",
            color    = AppColors.TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Finish", onClick = onFinish)
        Spacer(modifier = Modifier.height(12.dp))
        OptionButton(label = "Back",   onClick = onBack)
    }

    // Dialog guardar/subir .pkm
    if (mostrarDialogoPkm) {
        SavePkmDialog(
            onConfirm = { nombre, author, description ->
                mostrarDialogoPkm = false

                val contenido    = PkmExporter.exportar(formElements, author, description)
                val archivoLocal = guardarArchivoLocal(context, nombre, contenido)

                if (archivoLocal != null) {
                    Toast.makeText(
                        context,
                        "Guardado en Descargas/PKM_Forms/${archivoLocal.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    subiendo      = true
                    mensajeSubida = null
                    scope.launch {
                        val resultado = ApiService.subirPkm(context, archivoLocal)
                        subiendo = false
                        resultado.fold(
                            onSuccess = { nombreFinal ->
                                mensajeSubida = "Subido correctamente: $nombreFinal"
                                Toast.makeText(context, "Subido al servidor", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { error ->
                                mensajeSubida = "Error al subir: ${error.message}"
                                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            },
            onDismiss = { mostrarDialogoPkm = false }
        )
    }

    // Dialog guardar .form
    if (mostrarDialogoForm) {
        SaveFormDialog(
            onConfirm = { nombre ->
                mostrarDialogoForm = false
                guardarArchivoForm(context, codeText, nombre)
            },
            onDismiss = { mostrarDialogoForm = false }
        )
    }

    // Dialog configurar IP del servidor
    if (mostrarDialogoConfig) {
        ServerConfigDialog(
            ipActual  = ipActual,
            onConfirm = { nuevaIp ->
                ApiConfig.guardarIp(context, nuevaIp)
                ipActual          = nuevaIp
                mensajeSubida     = null
                mostrarDialogoConfig = false
                Toast.makeText(context, "Servidor actualizado: http://$nuevaIp:8080", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { mostrarDialogoConfig = false }
        )
    }

    // Indicador de carga mientras sube
    if (subiendo) {
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.Accent)
        }
    }
}


// Helpers

private fun guardarArchivoLocal(
    context: Context,
    nombre: String,
    contenido: String
): File? {
    return try {
        val descargas  = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val directorio = File(descargas, "PKM_Forms")
        if (!directorio.exists()) directorio.mkdirs()
        val nombreLimpio = nombre.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val archivo      = File(directorio, "$nombreLimpio.pkm")
        archivo.writeText(contenido)
        archivo
    } catch (e: Exception) {
        Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
        null
    }
}

private fun guardarArchivoForm(context: Context, codigo: String, nombre: String) {
    try {
        val descargas  = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val directorio = File(descargas, "PKM_Forms")
        if (!directorio.exists()) directorio.mkdirs()
        val nombreLimpio = nombre.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val archivo      = File(directorio, "$nombreLimpio.form")
        archivo.writeText(codigo)
        Toast.makeText(context, "Guardado en Descargas/PKM_Forms/${archivo.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun leerArchivoDesdeUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun OptionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.width(240.dp).height(48.dp),
        shape    = RoundedCornerShape(24.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = AppColors.OptionButton,
            disabledContainerColor = AppColors.OptionButton.copy(alpha = 0.35f)
        )
    ) {
        Text(
            text     = label,
            color    = if (enabled) AppColors.Text else AppColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}