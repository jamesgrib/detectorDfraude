package com.fraude.reporte.controller;

import com.fraude.reporte.service.ReporteService;
import com.fraude.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";

    private final ReporteService reporteService;
    private final UsuarioService usuarioService;

    public ReporteController(ReporteService reporteService, UsuarioService usuarioService) {
        this.reporteService = reporteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> resumenGeneral(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDoc) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDoc);
        if (authError != null)
            return authError;
        try {
            return ResponseEntity.ok(reporteService.obtenerResumenGeneral());
        } catch (Exception e) {
            log.error("Error en resumen general: {}", e.getMessage(), e);
            return error("Error al generar reporte");
        }
    }

    @GetMapping("/distribucion")
    public ResponseEntity<?> distribucion(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDoc) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDoc);
        if (authError != null)
            return authError;
        try {
            return ResponseEntity.ok(reporteService.distribucionPorEstado());
        } catch (Exception e) {
            log.error("Error en distribución: {}", e.getMessage(), e);
            return error("Error al generar distribución");
        }
    }

    @GetMapping("/top-cuentas")
    public ResponseEntity<?> topCuentas(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDoc) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDoc);
        if (authError != null)
            return authError;
        try {
            return ResponseEntity.ok(reporteService.topCuentasPorActividad());
        } catch (Exception e) {
            log.error("Error en top cuentas: {}", e.getMessage(), e);
            return error("Error al obtener top cuentas");
        }
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<?> reporteCuenta(@PathVariable String numeroCuenta) {
        try {
            return ResponseEntity.ok(reporteService.reportePorCuenta(numeroCuenta));
        } catch (Exception e) {
            log.error("Error en reporte de cuenta {}: {}", numeroCuenta, e.getMessage(), e);
            return error("Error al generar reporte de cuenta");
        }
    }

    private ResponseEntity<Map<String, Object>> validarAdmin(String adminDocumento) {
        if (adminDocumento == null || adminDocumento.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put(KEY_SUCCESS, false);
            err.put(KEY_MESSAGE, "Acceso denegado: se requiere autenticación de administrador");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        if (!usuarioService.esAdministrador(adminDocumento)) {
            Map<String, Object> err = new HashMap<>();
            err.put(KEY_SUCCESS, false);
            err.put(KEY_MESSAGE, "Acceso denegado: no tienes permisos de administrador");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> error(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put(KEY_SUCCESS, false);
        err.put(KEY_MESSAGE, msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
