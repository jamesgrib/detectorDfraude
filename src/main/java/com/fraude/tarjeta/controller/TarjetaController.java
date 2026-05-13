package com.fraude.tarjeta.controller;

import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.service.TarjetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/tarjetas")
@RequiredArgsConstructor
public class TarjetaController {

    private static final String KEY_MENSAJE        = "mensaje";
    private static final String KEY_TARJETA        = "tarjeta";
    private static final String KEY_LIMITE_CREDITO = "limiteCredito";

    private final TarjetaService tarjetaService;

    @GetMapping
    public ResponseEntity<?> obtenerTarjetas(
            @RequestHeader("X-User-Documento") String numDocumento) {
        return ResponseEntity.ok(tarjetaService.obtenerTarjetasUsuario(numDocumento));
    }

    @PostMapping
    public ResponseEntity<?> solicitarTarjeta(
            @RequestHeader("X-User-Documento") String numDocumento,
            @RequestBody Map<String, Object> body) {
        String nombreTitular = (String) body.get("nombreTitular");
        String tipoTarjeta   = (String) body.get("tipoTarjeta");
        Tarjeta tarjeta = tarjetaService.solicitarTarjeta(numDocumento, nombreTitular, tipoTarjeta);
        return ResponseEntity.ok(Map.of(
                KEY_MENSAJE, "Solicitud enviada. Espera la aprobacion del administrador.",
                KEY_TARJETA, buildTarjetaMap(tarjeta)));
    }

    @PostMapping("/{tarjetaId}/recargar")
    public ResponseEntity<?> recargarDebito(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer tarjetaId,
            @RequestBody Map<String, Object> body) {
        Double monto        = Double.valueOf(body.get("monto").toString());
        String numeroCuenta = (String) body.get("numeroCuenta");
        Tarjeta tarjeta = tarjetaService.recargarDebito(tarjetaId, numDocumento, monto, numeroCuenta);
        return ResponseEntity.ok(Map.of(
                KEY_MENSAJE,  "Recarga exitosa",
                "nuevoSaldo", tarjeta.getSaldoTarjeta(),
                KEY_TARJETA,  buildTarjetaMap(tarjeta)));
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
        Double limiteCredito = Optional.ofNullable(body)
                .map(b -> b.get(KEY_LIMITE_CREDITO))
                .map(v -> Double.valueOf(v.toString()))
                .orElse(null);
        Tarjeta tarjeta = tarjetaService.aprobarTarjeta(tarjetaId, limiteCredito);
        return ResponseEntity.ok(Map.of(
                KEY_MENSAJE, "Tarjeta aprobada exitosamente",
                KEY_TARJETA, buildTarjetaMap(tarjeta)));
    }

    @PostMapping("/{tarjetaId}/rechazar")
    public ResponseEntity<?> rechazarTarjeta(
            @PathVariable Integer tarjetaId,
            @RequestBody(required = false) Map<String, Object> body) {
        String motivo = Optional.ofNullable(body)
                .map(b -> (String) b.get("motivo"))
                .orElse(null);
        Tarjeta tarjeta = tarjetaService.rechazarTarjeta(tarjetaId, motivo);
        return ResponseEntity.ok(Map.of(
                KEY_MENSAJE, "Tarjeta rechazada",
                KEY_TARJETA, buildTarjetaMap(tarjeta)));
    }

    @DeleteMapping("/{tarjetaId}")
    public ResponseEntity<?> eliminarTarjeta(
            @RequestHeader("X-User-Documento") String numDocumento,
            @PathVariable Integer tarjetaId) {
        tarjetaService.eliminarTarjeta(tarjetaId, numDocumento);
        return ResponseEntity.ok(Map.of(KEY_MENSAJE, "Tarjeta eliminada exitosamente"));
    }

    private Map<String, Object> buildTarjetaMap(Tarjeta t) {
        return Map.ofEntries(
                Map.entry("id",                t.getId()),
                Map.entry("tipoTarjeta",       t.getTipoTarjeta()),
                Map.entry("marca",             t.getMarca()),
                Map.entry("ultimosCuatro",     t.getUltimosCuatro()),
                Map.entry("nombreTitular",     t.getNombreTitular()),
                Map.entry("fechaExpiracion",   t.getFechaExpiracion()),
                Map.entry("estadoId",          t.getEstadoId()),
                Map.entry("estadoNombre",      t.getEstadoNombre()),
                Map.entry(KEY_LIMITE_CREDITO,  Objects.requireNonNullElse(t.getLimiteCredito(), 0.0)),
                Map.entry("creditoDisponible", Objects.requireNonNullElse(t.getCreditoDisponible(), 0.0)),
                Map.entry("saldoTarjeta",      Objects.requireNonNullElse(t.getSaldoTarjeta(), 0.0))
        );
    }
}
