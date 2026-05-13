package com.fraude.factura.controller;

import com.fraude.factura.model.Factura;
import com.fraude.factura.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping
    public ResponseEntity<?> obtenerFacturas(
            @RequestHeader("X-User-Documento") String numDocumento) {
        return ResponseEntity.ok(facturaService.obtenerFacturas(numDocumento));
    }

    @PostMapping("/generar-prueba")
    public ResponseEntity<?> generarFacturasPrueba(
            @RequestHeader("X-User-Documento") String numDocumento,
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "mensaje",  "Facturas generadas",
                "facturas", facturaService.generarFacturasPrueba(numDocumento)));
    }

    @PostMapping("/{facturaId}/pagar")
    public ResponseEntity<?> pagarFactura(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer facturaId,
            @RequestBody Map<String, Object> body) {
        Integer tarjetaId   = Optional.ofNullable(body.get("tarjetaId"))
                .map(v -> Integer.valueOf(v.toString())).orElse(null);
        String numeroCuenta = (String) body.get("numeroCuenta");

        Factura factura = facturaService.pagarFactura(facturaId, numDocumento, tarjetaId, numeroCuenta);

        return ResponseEntity.ok(Map.of(
                "mensaje",     "Factura pagada exitosamente",
                "facturaId",   factura.getId(),
                "tipoServicio",factura.getTipoServicio(),
                "monto",       factura.getMonto(),
                "fechaPago",   factura.getFechaPago().toString(),
                "metodoPago",  tarjetaId != null ? "TARJETA" : "SALDO"));
    }
}
