$noBom = [System.Text.UTF8Encoding]::new($false)
$content = @'
package com.fraude.tarjeta.controller;

import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.service.TarjetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarjetas")
@RequiredArgsConstructor
@Slf4j
public class TarjetaController {

    private final TarjetaService tarjetaService;

    /** Usuario: ver sus propias tarjetas (todos los estados) */
    @GetMapping
    public ResponseEntity<?> obtenerTarjetas(
            @RequestHeader("X-User-Documento") String numDocumento) {
        List<Tarjeta> tarjetas = tarjetaService.obtenerTarjetasUsuario(numDocumento);
        return ResponseEntity.ok(tarjetas);
    }

    /**
     * Usuario: solicitar una nueva tarjeta (queda PENDIENTE).
     * Body: { tipoDocumentoId, nombreTitular, tipoTarjeta, numeroTarjeta, expMes, expAnio }
     */
    @PostMapping
    public ResponseEntity<?> solicitarTarjeta(
            @RequestHeader("X-User-Documento") String numDocumento,
            @RequestBody Map<String, Object> body) {
        try {
            Integer tipoDoc = (Integer) body.get("tipoDocumentoId");
            String nombreTitular = (String) body.get("nombreTitular");
            String tipoTarjeta = (String) body.get("tipoTarjeta");
            String numeroTarjeta = (String) body.get("numeroTarjeta");
            Long expMes = Long.valueOf(body.get("expMes").toString());
            Long expAnio = Long.valueOf(body.get("expAnio").toString());

            Tarjeta tarjeta = tarjetaService.solicitarTarjeta(
                    numDocumento, tipoDoc, nombreTitular, tipoTarjeta, numeroTarjeta, expMes, expAnio);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Solicitud enviada. Espera la aprobacion del administrador.",
                    "tarjeta", buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al solicitar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Usuario: recargar saldo en tarjeta DEBITO desde cuenta bancaria */
    @PostMapping("/{tarjetaId}/recargar")
    public ResponseEntity<?> recargarDebito(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer tarjetaId,
            @RequestBody Map<String, Object> body) {
        try {
            Double monto = Double.valueOf(body.get("monto").toString());
            String numeroCuenta = (String) body.get("numeroCuenta");

            Tarjeta tarjeta = tarjetaService.recargarDebito(tarjetaId, numDocumento, monto, numeroCuenta);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Recarga exitosa",
                    "nuevoSaldo", tarjeta.getSaldoTarjeta(),
                    "tarjeta", buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al recargar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Admin: ver todas las tarjetas pendientes */
    @GetMapping("/admin/pendientes")
    public ResponseEntity<?> obtenerPendientes() {
        return ResponseEntity.ok(tarjetaService.obtenerPendientes());
    }

    /** Admin: ver todas las tarjetas del sistema */
    @GetMapping("/admin/todas")
    public ResponseEntity<?> obtenerTodas() {
        return ResponseEntity.ok(tarjetaService.obtenerTodas());
    }

    /** Admin: aprobar solicitud de tarjeta */
    @PostMapping("/{tarjetaId}/aprobar")
    public ResponseEntity<?> aprobarTarjeta(
            @PathVariable Integer tarjetaId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Double limiteCredito = body != null && body.get("limiteCredito") != null
                    ? Double.valueOf(body.get("limiteCredito").toString()) : null;
            Tarjeta tarjeta = tarjetaService.aprobarTarjeta(tarjetaId, limiteCredito);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Tarjeta aprobada exitosamente",
                    "tarjeta", buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al aprobar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Admin: rechazar solicitud de tarjeta */
    @PostMapping("/{tarjetaId}/rechazar")
    public ResponseEntity<?> rechazarTarjeta(
            @PathVariable Integer tarjetaId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String motivo = body != null ? (String) body.get("motivo") : null;
            Tarjeta tarjeta = tarjetaService.rechazarTarjeta(tarjetaId, motivo);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Tarjeta rechazada",
                    "tarjeta", buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al rechazar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Usuario: eliminar (cancelar) una tarjeta propia */
    @DeleteMapping("/{tarjetaId}")
    public ResponseEntity<?> eliminarTarjeta(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer tarjetaId) {
        try {
            tarjetaService.eliminarTarjeta(tarjetaId, numDocumento);
            return ResponseEntity.ok(Map.of("mensaje", "Tarjeta eliminada exitosamente"));
        } catch (Exception e) {
            log.error("Error al eliminar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> buildTarjetaMap(Tarjeta t) {
        return Map.of(
                "id", t.getId(),
                "tipoTarjeta", t.getTipoTarjeta(),
                "marca", t.getMarca(),
                "ultimosCuatro", t.getUltimosCuatro(),
                "nombreTitular", t.getNombreTitular(),
                "fechaExpiracion", t.getFechaExpiracion(),
                "estadoId", t.getEstadoId(),
                "limiteCredito", t.getLimiteCredito() != null ? t.getLimiteCredito() : 0.0,
                "creditoDisponible", t.getCreditoDisponible() != null ? t.getCreditoDisponible() : 0.0,
                "saldoTarjeta", t.getSaldoTarjeta() != null ? t.getSaldoTarjeta() : 0.0);
    }
}
'@
[System.IO.File]::WriteAllText("c:\Users\mater\Desktop\fraude-detection\src\main\java\com\fraude\tarjeta\controller\TarjetaController.java", $content, $noBom)
Write-Host "TarjetaController written OK"
