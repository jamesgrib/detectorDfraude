package com.fraude.reporte.service;

import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.transaccion.repository.TransaccionRepository;
import com.fraude.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ReporteService {

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteService(TransaccionRepository transaccionRepository,
            CuentaRepository cuentaRepository,
            UsuarioRepository usuarioRepository) {
        this.transaccionRepository = transaccionRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Reporte general del sistema
     */
    public Map<String, Object> obtenerResumenGeneral() {
        Map<String, Object> resumen = new LinkedHashMap<>();

        long totalTransacciones = transaccionRepository.count();
        long aprobadas = transaccionRepository.countByEstadoTransaccionNombre("APROBADA");
        long rechazadas = transaccionRepository.countByEstadoTransaccionNombre("RECHAZADA");
        long pendientes = transaccionRepository.countByEstadoTransaccionNombre("PENDIENTE");

        double montoAprobado = transaccionRepository.sumMontoAprobadas();
        double montoRechazado = transaccionRepository.sumMontoByEstadoNombre("RECHAZADA");
        double montoPendiente = transaccionRepository.sumMontoByEstadoNombre("PENDIENTE");

        long totalClientes = usuarioRepository.count();
        long totalCuentas = cuentaRepository.count();

        // Tasa de fraude (pendientes + rechazadas sobre total)
        double tasaFraude = totalTransacciones > 0
                ? Math.round(((double) (pendientes + rechazadas) / totalTransacciones) * 10000.0) / 100.0
                : 0.0;

        resumen.put("totalTransacciones", totalTransacciones);
        resumen.put("aprobadas", aprobadas);
        resumen.put("rechazadas", rechazadas);
        resumen.put("pendientes", pendientes);
        resumen.put("montoAprobado", montoAprobado);
        resumen.put("montoRechazado", montoRechazado);
        resumen.put("montoPendiente", montoPendiente);
        resumen.put("totalClientes", totalClientes);
        resumen.put("totalCuentas", totalCuentas);
        resumen.put("tasaFraudePorc", tasaFraude);

        return resumen;
    }

    /**
     * Distribución de transacciones por estado para gráfico de torta
     */
    public List<Map<String, Object>> distribucionPorEstado() {
        List<Map<String, Object>> lista = new ArrayList<>();

        String[] nombres = { "Pendiente", "Aprobada", "Rechazada" };
        String[] estadosNombre = { "PENDIENTE", "APROBADA", "RECHAZADA" };
        String[] colores = { "#f59e0b", "#10b981", "#ef4444" };

        for (int i = 0; i < estadosNombre.length; i++) {
            long count = transaccionRepository.countByEstadoTransaccionNombre(estadosNombre[i]);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("estado", nombres[i]);
            item.put("cantidad", count);
            item.put("color", colores[i]);
            lista.add(item);
        }
        return lista;
    }

    /**
     * Top cuentas por actividad (más transacciones enviadas aprobadas)
     */
    public List<Map<String, Object>> topCuentasPorActividad() {
        List<Object[]> raw = transaccionRepository.topCuentasPorActividad();
        List<Map<String, Object>> resultado = new ArrayList<>();

        int limit = Math.min(raw.size(), 5);
        for (int i = 0; i < limit; i++) {
            Object[] row = raw.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cuenta", row[0]);
            item.put("totalTransacciones", row[1]);
            item.put("montoTotal", row[2]);
            resultado.add(item);
        }
        return resultado;
    }

    /**
     * Reporte de actividad para una cuenta específica
     */
    public Map<String, Object> reportePorCuenta(String numeroCuenta) {
        Map<String, Object> reporte = new LinkedHashMap<>();

        var transacciones = transaccionRepository.findByCuenta(numeroCuenta);

        long enviadas = transacciones.stream()
                .filter(t -> numeroCuenta.equals(t.getCuentaOrigenId())).count();
        long recibidas = transacciones.stream()
                .filter(t -> numeroCuenta.equals(t.getCuentaDestinoId())).count();

        double montoEnviado = transacciones.stream()
                .filter(t -> numeroCuenta.equals(t.getCuentaOrigenId()) && t.getEstadoId() == 5)
                .mapToDouble(t -> t.getMonto() != null ? t.getMonto() : 0)
                .sum();
        double montoRecibido = transacciones.stream()
                .filter(t -> numeroCuenta.equals(t.getCuentaDestinoId()) && t.getEstadoId() == 5)
                .mapToDouble(t -> t.getMonto() != null ? t.getMonto() : 0)
                .sum();

        reporte.put("numeroCuenta", numeroCuenta);
        reporte.put("totalOperaciones", transacciones.size());
        reporte.put("transaccionesEnviadas", enviadas);
        reporte.put("transaccionesRecibidas", recibidas);
        reporte.put("montoEnviado", Math.round(montoEnviado * 100.0) / 100.0);
        reporte.put("montoRecibido", Math.round(montoRecibido * 100.0) / 100.0);
        reporte.put("transacciones", transacciones);

        return reporte;
    }
}
