package com.example.pkmforms.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pkmforms.analyzer.PkmImporter
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.api.ApiService
import com.example.pkmforms.api.FormularioInfo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExploreFormsScreen(
    onBack: () -> Unit,
    onFormDescargado: (String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var formularios  by remember { mutableStateOf<List<FormularioInfo>>(emptyList()) }
    var cargando     by remember { mutableStateOf(false) }
    var descargando  by remember { mutableStateOf<String?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    // Estado para el dialog de eleccion
    var formularioSeleccionado by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }

    // Cargar lista al entrar a la pantalla
    LaunchedEffect(Unit) {
        cargando     = true
        mensajeError = null
        val resultado = ApiService.listarFormularios(context)
        cargando = false
        resultado.fold(
            onSuccess = { lista ->
                formularios  = lista
                if (lista.isEmpty()) mensajeError = "No hay formularios en el servidor"
            },
            onFailure = { error ->
                mensajeError = error.message
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "Explore Forms",
                color      = AppColors.Text,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Boton refrescar
                TextButton(
                    onClick = {
                        scope.launch {
                            cargando     = true
                            mensajeError = null
                            val resultado = ApiService.listarFormularios(context)
                            cargando = false
                            resultado.fold(
                                onSuccess = { lista ->
                                    formularios  = lista
                                    if (lista.isEmpty()) mensajeError = "No hay formularios en el servidor"
                                },
                                onFailure = { error -> mensajeError = error.message }
                            )
                        }
                    }
                ) {
                    Text("Refresh", color = AppColors.Accent)
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = AppColors.TextSecondary)
                }
            }
        }

        // Contenido
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = AppColors.Accent
                    )
                }

                mensajeError != null && formularios.isEmpty() -> {
                    Column(
                        modifier              = Modifier.align(Alignment.Center),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text     = mensajeError ?: "",
                            color    = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    cargando     = true
                                    mensajeError = null
                                    val resultado = ApiService.listarFormularios(context)
                                    cargando = false
                                    resultado.fold(
                                        onSuccess = { lista -> formularios = lista },
                                        onFailure = { error -> mensajeError = error.message }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                            shape  = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reintentar", color = AppColors.Text)
                        }
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(formularios) { formulario ->
                            FormularioCard(
                                formulario  = formulario,
                                descargando = descargando == formulario.nombre,
                                onDescargar = {
                                    descargando = formulario.nombre
                                    scope.launch {
                                        val resultado = ApiService.descargarPkm(context, formulario.nombre)
                                        descargando = null
                                        resultado.fold(
                                            onSuccess = { bytes ->
                                                // Guardar localmente y mostrar dialog de eleccion
                                                try {
                                                    val descargas  = android.os.Environment
                                                        .getExternalStoragePublicDirectory(
                                                            android.os.Environment.DIRECTORY_DOWNLOADS
                                                        )
                                                    val directorio = File(descargas, "PKM_Forms")
                                                    if (!directorio.exists()) directorio.mkdirs()
                                                    val archivo = File(directorio, formulario.nombre)
                                                    archivo.writeBytes(bytes)
                                                    // Mostrar dialog de eleccion
                                                    formularioSeleccionado = Pair(formulario.nombre, bytes)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Contestar
    formularioSeleccionado?.let { (nombre, bytes) ->
        val contenido = bytes.toString(Charsets.UTF_8)
        Dialog(onDismissRequest = { formularioSeleccionado = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Surface)
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text       = nombre,
                    color      = AppColors.Text,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = "Formulario descargado correctamente.",
                    color    = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            formularioSeleccionado = null
                            onFormDescargado(contenido)
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) {
                        Text("Contestar", color = AppColors.Text)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormularioCard(
    formulario:  FormularioInfo,
    descargando: Boolean,
    onDescargar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.CodeBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Info del formulario
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text       = formulario.nombre,
                color      = AppColors.Text,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text     = formulario.tamano,
                    color    = AppColors.TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text     = formulario.fecha,
                    color    = AppColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Boton descargar
        if (descargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color    = AppColors.Accent,
                strokeWidth = 2.dp
            )
        } else {
            Button(
                onClick = onDescargar,
                colors  = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                shape   = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Download", color = AppColors.Text, fontSize = 13.sp)
            }
        }
    }
}