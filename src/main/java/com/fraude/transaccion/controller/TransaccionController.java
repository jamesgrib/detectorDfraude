package com.fraude.transaccion.controller;

import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.service.TransaccionService;
import com.fraude.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transacciones")
@CrossOrigin(origins = { "http://localhost:8081", "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:5174", "http://127.0.0.1:5174" })
public class TransaccionController {

    private static final String ESTADO_APROBADA  = "APROBADA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";

    private final TransaccionService service;
    private final UsuarioService usuarioService;

    public TransaccionController(TransaccionService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> procesarTransaccion(@RequestBody Transaccion transaccion) {
        log.info("Nueva solicitud de transaccion recibida");
        Transaccion resultado = service.procesarTransaccion(transaccion);
        log.info("Transaccion procesada exitosamente");
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<?> obtenerHistorial(@PathVariable String numeroCuenta) {
        log.info("Solicitud de historial para cuenta: {}", numeroCuenta);
        return ResponseEntity.ok(service.obtenerHistorial(numeroCuenta));
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodasTransacciones(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        validarAdmin(adminDocumento);
        log.info("Solicitud admin de todas las transacciones");
        return ResponseEntity.ok(service.obtenerTodasTransacciones());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerTransaccionesPendientes(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        validarAdmin(adminDocumento);
        log.info("Solicitud de transacciones pendientes (Admin)");
        return ResponseEntity.ok(service.obtenerTransaccionesPendientes());
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstadoTransaccion(
            @PathVariable Integer id,
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento,
            @RequestBody Map<String, Object> body) {
        validarAdmin(adminDocumento);
        String nuevoEstadoNombre = resolverEstadoNombre(body);
        validarEstadoFinal(nuevoEstadoNombre);
        log.info("Actualizacion de estado: id={}, estado={}", id, nuevoEstadoNombre);
        return ResponseEntity.ok(service.actualizarEstadoTransaccion(id, nuevoEstadoNombre));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validarAdmin(String adminDocumento) {
        if (adminDocumento == null || adminDocumento.isBlank()) {
            throw new IllegalArgumentException("Header X-Admin-Documento es requerido");
        }
        if (!usuarioService.esAdministrador(adminDocumento)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo un administrador puede ejecutar esta accion");
        }
    }

    private void validarEstadoFinal(String estadoNombre) {
        if (!ESTADO_APROBADA.equals(estadoNombre) && !ESTADO_RECHAZADA.equals(estadoNombre)) {
            throw new IllegalArgumentException("Estado invalido. Debe ser APROBADA o RECHAZADA");
        }
    }

    private String resolverEstadoNombre(Map<String, Object> body) {
        if (body.containsKey("estadoNombre")) {
            return (String) body.get("estadoNombre");
        }
        if (body.containsKey("estadoId")) {
            int legacyId = toLegacyId(body.get("estadoId"));
            if (legacyId == 5) return ESTADO_APROBADA;
            if (legacyId == 6) return ESTADO_RECHAZADA;
        }
        return null;
    }

    private int toLegacyId(Object raw) {
        return raw instanceof Number ? ((Number) raw).intValue() : Integer.parseInt(raw.toString());
    }
}
