package com.fraude.transaccion.controller;

import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.service.TransaccionService;
import com.fraude.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transacciones")
@CrossOrigin(origins = { "http://localhost:8081", "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:5174", "http://127.0.0.1:5174" })
public class TransaccionController {

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_STATUS  = "status";

    private final TransaccionService service;
    private final UsuarioService usuarioService;

    public TransaccionController(TransaccionService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> procesarTransaccion(@RequestBody Transaccion transaccion) {
        try {
            log.info("Nueva solicitud de transaccion recibida");
            Transaccion resultado = service.procesarTransaccion(transaccion);
            log.info("Transaccion procesada exitosamente");
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Validacion fallida: {}", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Error al procesar transaccion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Error al procesar la transaccion: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<?> obtenerHistorial(@PathVariable String numeroCuenta) {
        try {
            log.info("Solicitud de historial para cuenta: {}", numeroCuenta);
            List<Transaccion> historial = service.obtenerHistorial(numeroCuenta);
            log.info("Historial obtenido: {} transacciones", historial.size());
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            log.error("Error al obtener historial: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Error al obtener el historial", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodasTransacciones(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDocumento);
        if (authError != null) return authError;
        try {
            log.info("Solicitud admin de todas las transacciones");
            return ResponseEntity.ok(service.obtenerTodasTransacciones());
        } catch (Exception e) {
            log.error("Error al obtener todas las transacciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Error al obtener transacciones", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerTransaccionesPendientes(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDocumento);
        if (authError != null) return authError;
        try {
            log.info("Solicitud de transacciones pendientes (Admin)");
            List<Transaccion> pendientes = service.obtenerTransaccionesPendientes();
            log.info("Transacciones pendientes obtenidas: {}", pendientes.size());
            return ResponseEntity.ok(pendientes);
        } catch (Exception e) {
            log.error("Error al obtener transacciones pendientes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Error al obtener transacciones pendientes", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstadoTransaccion(
            @PathVariable Integer id,
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento,
            @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDocumento);
        if (authError != null) return authError;

        try {
            String nuevoEstadoNombre = resolverEstadoNombre(body);

            if (!"APROBADA".equals(nuevoEstadoNombre) && !"RECHAZADA".equals(nuevoEstadoNombre)) {
                throw new IllegalArgumentException("Estado invalido. Debe ser APROBADA o RECHAZADA");
            }

            log.info("Actualizacion de estado: id={}, estado={}", id, nuevoEstadoNombre);
            Transaccion actualizada = service.actualizarEstadoTransaccion(id, nuevoEstadoNombre);
            log.info("Estado actualizado: id={}, estado={}", id, nuevoEstadoNombre);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            log.warn("Validacion fallida: {}", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Error al actualizar estado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Error al actualizar el estado", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolverEstadoNombre(Map<String, Object> body) {
        if (body.containsKey("estadoNombre")) {
            return (String) body.get("estadoNombre");
        }
        if (body.containsKey("estadoId")) {
            Object raw = body.get("estadoId");
            int legacyId = raw instanceof Number ? ((Number) raw).intValue()
                    : Integer.parseInt(raw.toString());
            if (legacyId == 5) return "APROBADA";
            if (legacyId == 6) return "RECHAZADA";
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> validarAdmin(String adminDocumento) {
        if (adminDocumento == null || adminDocumento.isBlank()) {
            return construirError(HttpStatus.BAD_REQUEST, "Header X-Admin-Documento es requerido");
        }
        if (!usuarioService.esAdministrador(adminDocumento)) {
            return construirError(HttpStatus.FORBIDDEN, "Solo un administrador puede ejecutar esta accion");
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> construirError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(errorBody(message, status));
    }

    private Map<String, Object> errorBody(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put(KEY_SUCCESS, false);
        error.put(KEY_MESSAGE, message);
        error.put(KEY_STATUS,  status.value());
        return error;
    }
}
