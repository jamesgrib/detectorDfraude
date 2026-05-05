package com.fraude.factura.controller;

import com.fraude.factura.model.Factura;
import com.fraude.factura.service.FacturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@Slf4j
public class FacturaController {

    private final FacturaService facturaService;

    /** Obtener todas las facturas del usuario */
    @GetMapping
    public ResponseEntity<?> obtenerFacturas(
            @RequestHeader("X-User-Documento") String numDocumento) {
        List<Factura> facturas = facturaService.obtenerFacturas(numDocumento);
        return ResponseEntity.ok(facturas);
    }

    /**
     * Generar facturas de prueba para el usuario.
     * Crea facturas de servicios (luz, agua, gas, internet, teléfono) si no
     * existen.
     */
    @PostMapping("/generar-prueba")
    public ResponseEntity<?> generarFacturasPrueba(
            @RequestHeader("X-User-Documento") String numDocumento,
            @RequestBody(required = false) Map<String, Object> body) {
        List<Factura> facturas = facturaService.generarFacturasPrueba(numDocumento);
        return ResponseEntity.ok(Map.of("mensaje", "Facturas generadas", "facturas", facturas));
    }

    /**
     * Pagar una factura.
     * Body esperado:
     * {
     * "tarjetaId": 1, // opcional — si paga con tarjeta
     * "numeroCuenta": "ACC-001" // opcional — si paga con saldo
     * }
     * Debe indicarse al menos uno de los dos.
     */
    @PostMapping("/{facturaId}/pagar")
    public ResponseEntity<?> pagarFactura(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer facturaId,
            @RequestBody Map<String, Object> body) {
        try {
            Integer tarjetaId = body.get("tarjetaId") != null
                    ? Integer.valueOf(body.get("tarjetaId").toString())
                    : null;
            String numeroCuenta = (String) body.get("numeroCuenta");

            Factura factura = facturaService.pagarFactura(facturaId, numDocumento, tarjetaId, numeroCuenta);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Factura pagada exitosamente",
                    "facturaId", factura.getId(),
                    "tipoServicio", factura.getTipoServicio(),
                    "monto", factura.getMonto(),
                    "fechaPago", factura.getFechaPago().toString(),
                    "metodoPago", tarjetaId != null ? "TARJETA" : "SALDO"));
        } catch (Exception e) {
            log.error("Error al pagar factura: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
