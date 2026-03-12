package com.pkmforms.api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/formularios")
public class FormularioController {

    // Carpeta donde se guardan los .pkm
    private static final String CARPETA = "formularios";

    // =========================================================================
    // SUBIR .pkm
    // POST /formularios/subir
    // Body: multipart/form-data con campo "archivo"
    // =========================================================================
    @PostMapping("/subir")
    public ResponseEntity<Map<String, String>> subir(
            @RequestParam("archivo") MultipartFile archivo
    ) {
        Map<String, String> respuesta = new HashMap<>();

        // Validar que sea un archivo .pkm
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.endsWith(".pkm")) {
            respuesta.put("error", "Solo se permiten archivos .pkm");
            return ResponseEntity.badRequest().body(respuesta);
        }

        // Validar que no este vacio
        if (archivo.isEmpty()) {
            respuesta.put("error", "El archivo esta vacio");
            return ResponseEntity.badRequest().body(respuesta);
        }

        try {
            // Crear carpeta si no existe
            File carpeta = new File(CARPETA);
            if (!carpeta.exists()) carpeta.mkdirs();

            // Si ya existe un archivo con ese nombre, agregar timestamp
            String nombreFinal = nombreOriginal;
            File destino = new File(carpeta, nombreFinal);
            if (destino.exists()) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                nombreFinal = nombreOriginal.replace(".pkm", "_" + timestamp + ".pkm");
                destino = new File(carpeta, nombreFinal);
            }

            // Guardar archivo
            Path ruta = Paths.get(CARPETA, nombreFinal);
            Files.write(ruta, archivo.getBytes());

            respuesta.put("mensaje",  "Archivo guardado correctamente");
            respuesta.put("nombre",   nombreFinal);
            respuesta.put("tamano",   archivo.getSize() + " bytes");
            return ResponseEntity.ok(respuesta);

        } catch (IOException e) {
            respuesta.put("error", "Error al guardar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    // =========================================================================
    // LISTAR formularios disponibles
    // GET /formularios/lista
    // =========================================================================
    @GetMapping("/lista")
    public ResponseEntity<Map<String, Object>> lista() {
        Map<String, Object> respuesta = new HashMap<>();

        File carpeta = new File(CARPETA);
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            respuesta.put("formularios", Collections.emptyList());
            respuesta.put("total", 0);
            return ResponseEntity.ok(respuesta);
        }

        File[] archivos = carpeta.listFiles((dir, nombre) -> nombre.endsWith(".pkm"));
        if (archivos == null) archivos = new File[0];

        // Ordenar por fecha de modificacion (mas reciente primero)
        Arrays.sort(archivos, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        List<Map<String, String>> lista = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (File f : archivos) {
            Map<String, String> info = new HashMap<>();
            info.put("nombre", f.getName());
            info.put("tamano", f.length() + " bytes");
            info.put("fecha",  sdf.format(new Date(f.lastModified())));
            lista.add(info);
        }

        respuesta.put("formularios", lista);
        respuesta.put("total", lista.size());
        return ResponseEntity.ok(respuesta);
    }

    // =========================================================================
    // DESCARGAR un .pkm especifico
    // GET /formularios/descargar/{nombre}
    // =========================================================================
    @GetMapping("/descargar/{nombre}")
    public ResponseEntity<byte[]> descargar(@PathVariable String nombre) {

        // Validar nombre para evitar path traversal
        if (nombre.contains("..") || nombre.contains("/") || !nombre.endsWith(".pkm")) {
            return ResponseEntity.badRequest().build();
        }

        File archivo = new File(CARPETA, nombre);
        if (!archivo.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] contenido = Files.readAllBytes(archivo.toPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", nombre);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(contenido);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // ELIMINAR un .pkm
    // DELETE /formularios/eliminar/{nombre}
    // =========================================================================
    @DeleteMapping("/eliminar/{nombre}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String nombre) {
        Map<String, String> respuesta = new HashMap<>();

        if (nombre.contains("..") || nombre.contains("/") || !nombre.endsWith(".pkm")) {
            respuesta.put("error", "Nombre de archivo invalido");
            return ResponseEntity.badRequest().body(respuesta);
        }

        File archivo = new File(CARPETA, nombre);
        if (!archivo.exists()) {
            respuesta.put("error", "Archivo no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
        }

        if (archivo.delete()) {
            respuesta.put("mensaje", "Archivo eliminado: " + nombre);
            return ResponseEntity.ok(respuesta);
        } else {
            respuesta.put("error", "No se pudo eliminar el archivo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }
}
