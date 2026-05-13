package com.fraude.reporte.controller;

import com.fraude.reporte.service.ReporteService;
import com.fraude.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioService usuarioService;

    public ReporteController(ReporteService reporteService, UsuarioService usuarioService) {
        this.reporteService = reporteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> resumenGeneral(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDoc) {
        validarAdmin(adminDoc);
        return ResponseEntity.ok(reporteService.obtenerResumenGeneral());
    }

    @GetMapping("/distribucion")
    public ResponseEntity<?> distribucion(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDoc) {
        validarAdmin(adminDoc);
        return ResponseEntity.ok(reporteService.distribucionPorEstado());
    }

    @GetMapping("/top-cuentas")
    public ResponseEntity<?> topCuentas(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDoc) {
        validarAdmin(adminDoc);
        return ResponseEntity.ok(reporteService.topCuentasPorActividad());
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<?> reporteCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(reporteService.reportePorCuenta(numeroCuenta));
    }

    private void validarAdmin(String adminDocumento) {
        if (adminDocumento == null || adminDocumento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Acceso denegado: se requiere autenticacion de administrador");
        }
        if (!usuarioService.esAdministrador(adminDocumento)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acceso denegado: no tienes permisos de administrador");
        }
    }
}
