package com.fraude.tarjeta.controller;

import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.service.TarjetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarjetas")
@RequiredArgsConstructor
@Slf4j
public class TarjetaController {

    private static final String KEY_MENSAJE       = "mensaje";
    private static final String KEY_TARJETA       = "tarjeta";
    private static final String KEY_ERROR         = "error";
    private static final String KEY_LIMITE_CREDITO = "limiteCredito";

    private final TarjetaService tarjetaService;

    @GetMapping
    public ResponseEntity<?> obtenerTarjetas(
            @RequestHeader("X-User-Documento") String numDocumento) {
        List<Tarjeta> tarjetas = tarjetaService.obtenerTarjetasUsuario(numDocumento);
        return ResponseEntity.ok(tarjetas);
    }

    @PostMapping
    public ResponseEntity<?> solicitarTarjeta(
            @RequestHeader("X-User-Documento") String numDocumento,
            @RequestBody Map<String, Object> body) {
        try {
            String nombreTitular = (String) body.get("nombreTitular");
            String tipoTarjeta   = (String) body.get("tipoTarjeta");
            Tarjeta tarjeta = tarjetaService.solicitarTarjeta(numDocumento, nombreTitular, tipoTarjeta);
            return ResponseEntity.ok(Map.of(
                    KEY_MENSAJE, "Solicitud enviada. Espera la aprobacion del administrador.",
                    KEY_TARJETA, buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al solicitar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/{tarjetaId}/recargar")
    public ResponseEntity<?> recargarDebito(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer tarjetaId,
            @RequestBody Map<String, Object> body) {
        try {
            Double monto        = Double.valueOf(body.get("monto").toString());
            String numeroCuenta = (String) body.get("numeroCuenta");
            Tarjeta tarjeta = tarjetaService.recargarDebito(tarjetaId, numDocumento, monto, numeroCuenta);
            return ResponseEntity.ok(Map.of(
                    KEY_MENSAJE,  "Recarga exitosa",
                    "nuevoSaldo", tarjeta.getSaldoTarjeta(),
                    KEY_TARJETA,  buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al recargar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @GetMapping("/admin/pendientes")
    public ResponseEntity<?> obtenerPendientes() {
        return ResponseEntity.ok(tarjetaService.obtenerPendientes());
    }

    @GetMapping("/admin/todas")
    public ResponseEntity<?> obtenerTodas() {
        return ResponseEntity.ok(tarjetaService.obtenerTodas());
    }

    @PostMapping("/{tarjetaId}/aprobar")
    public ResponseEntity<?> aprobarTarjeta(
            @PathVariable Integer tarjetaId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Double limiteCredito = body != null && body.get(KEY_LIMITE_CREDITO) != null
                    ? Double.valueOf(body.get(KEY_LIMITE_CREDITO).toString())
                    : null;
            Tarjeta tarjeta = tarjetaService.aprobarTarjeta(tarjetaId, limiteCredito);
            return ResponseEntity.ok(Map.of(
                    KEY_MENSAJE, "Tarjeta aprobada exitosamente",
                    KEY_TARJETA, buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al aprobar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/{tarjetaId}/rechazar")
    public ResponseEntity<?> rechazarTarjeta(
            @PathVariable Integer tarjetaId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String motivo = body != null ? (String) body.get("motivo") : null;
            Tarjeta tarjeta = tarjetaService.rechazarTarjeta(tarjetaId, motivo);
            return ResponseEntity.ok(Map.of(
                    KEY_MENSAJE, "Tarjeta rechazada",
                    KEY_TARJETA, buildTarjetaMap(tarjeta)));
        } catch (Exception e) {
            log.error("Error al rechazar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @DeleteMapping("/{tarjetaId}")
    public ResponseEntity<?> eliminarTarjeta(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer tarjetaId) {
        try {
            tarjetaService.eliminarTarjeta(tarjetaId, numDocumento);
            return ResponseEntity.ok(Map.of(KEY_MENSAJE, "Tarjeta eliminada exitosamente"));
        } catch (Exception e) {
            log.error("Error al eliminar tarjeta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    private Map<String, Object> buildTarjetaMap(Tarjeta t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",               t.getId());
        map.put("tipoTarjeta",      t.getTipoTarjeta());
        map.put("marca",            t.getMarca());
        map.put("ultimosCuatro",    t.getUltimosCuatro());
        map.put("nombreTitular",    t.getNombreTitular());
        map.put("fechaExpiracion",  t.getFechaExpiracion());
        map.put("estadoId",         t.getEstadoId());
        map.put("estadoNombre",     t.getEstadoNombre());
        map.put(KEY_LIMITE_CREDITO, t.getLimiteCredito()      != null ? t.getLimiteCredito()      : 0.0);
        map.put("creditoDisponible",t.getCreditoDisponible()  != null ? t.getCreditoDisponible()  : 0.0);
        map.put("saldoTarjeta",     t.getSaldoTarjeta()       != null ? t.getSaldoTarjeta()       : 0.0);
        return map;
    }
}
