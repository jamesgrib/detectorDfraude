package com.fraude.transaccion.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.TipoTransaccion;
import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.repository.EstadoTransaccionRepository;
import com.fraude.transaccion.repository.TipoTransaccionRepository;
import com.fraude.transaccion.repository.TransaccionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class TransaccionService {

    private static final String ESTADO_APROBADA  = "APROBADA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";

    private final TransaccionRepository transaccionRepository;
    private final FraudeService fraudeService;
    private final CuentaRepository cuentaRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final EstadoTransaccionRepository estadoTransaccionRepository;

    public TransaccionService(TransaccionRepository transaccionRepository, FraudeService fraudeService,
            CuentaRepository cuentaRepository, TipoTransaccionRepository tipoTransaccionRepository,
            EstadoTransaccionRepository estadoTransaccionRepository) {
        this.transaccionRepository = transaccionRepository;
        this.fraudeService = fraudeService;
        this.cuentaRepository = cuentaRepository;
        this.tipoTransaccionRepository = tipoTransaccionRepository;
        this.estadoTransaccionRepository = estadoTransaccionRepository;
    }

    private TipoTransaccion getTipo(String nombre) {
        String key = (nombre != null && !nombre.isBlank()) ? nombre.toUpperCase() : "TRANSFERENCIA";
        return tipoTransaccionRepository.findByNombre(key)
                .orElseGet(() -> tipoTransaccionRepository.findByNombre("TRANSFERENCIA")
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Tipo de transaccion no encontrado: " + key)));
    }

    private EstadoTransaccion getEstado(String nombre) {
        return estadoTransaccionRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estado de transaccion no encontrado: " + nombre));
    }

    private void validarInputsTransaccion(Transaccion transaccion) {
        if (transaccion.getMonto() == null || transaccion.getMonto() <= 0) {
            throw new IllegalArgumentException("Monto debe ser mayor a 0");
        }
        if (transaccion.getCuentaOrigenId() == null || transaccion.getCuentaOrigenId().isEmpty()) {
            throw new IllegalArgumentException("Cuenta origen es requerida");
        }
        if (transaccion.getCuentaDestinoId() == null || transaccion.getCuentaDestinoId().isEmpty()) {
            throw new IllegalArgumentException("Cuenta destino es requerida");
        }
    }

    private void validarTransicionEstado(Transaccion transaccion, String nuevoEstadoNombre) {
        if (!ESTADO_PENDIENTE.equals(transaccion.getEstadoNombre())) {
            throw new IllegalArgumentException(
                    "Solo se pueden validar transacciones en estado PENDIENTE");
        }
        if (!ESTADO_APROBADA.equals(nuevoEstadoNombre) && !ESTADO_RECHAZADA.equals(nuevoEstadoNombre)) {
            throw new IllegalArgumentException("Estado invalido. Debe ser APROBADA o RECHAZADA");
        }
    }

    private void actualizarSaldosCuentas(Cuenta origen, Cuenta destino, BigDecimal monto) {
        origen.setSaldo(origen.getSaldo().subtract(monto));
        destino.setSaldo(destino.getSaldo().add(monto));
        cuentaRepository.save(origen);
        cuentaRepository.save(destino);
    }

    private void aplicarTransferenciaAprobada(Cuenta origen, Cuenta destino,
            BigDecimal monto, Transaccion transaccion) {
        if (origen.getSaldo().compareTo(monto) < 0) {
            log.error("Saldo insuficiente: disponible={}, requerido={}", origen.getSaldo(), monto);
            transaccion.setEstadoTransaccion(getEstado(ESTADO_RECHAZADA));
            return;
        }
        actualizarSaldosCuentas(origen, destino, monto);
        log.info("Saldos actualizados. Origen: {}, Destino: {}", origen.getSaldo(), destino.getSaldo());
    }

    private void ejecutarTransferencia(Transaccion transaccion) {
        Cuenta origen  = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
        Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));
        BigDecimal monto = BigDecimal.valueOf(transaccion.getMonto());
        if (origen.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente en la cuenta origen");
        }
        actualizarSaldosCuentas(origen, destino, monto);
    }

    @Transactional
    public Transaccion procesarTransaccion(Transaccion transaccion) {
        log.info("Procesando transaccion: origen={}, destino={}, monto={}",
                transaccion.getCuentaOrigenId(),
                transaccion.getCuentaDestinoId(),
                transaccion.getMonto());

        validarInputsTransaccion(transaccion);

        Cuenta origen  = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
        Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));

        String estadoNombre = fraudeService.evaluarFraude(transaccion);
        log.info("Estado de fraude evaluado: {}", estadoNombre);
        transaccion.setEstadoTransaccion(getEstado(estadoNombre));

        if (ESTADO_APROBADA.equals(estadoNombre)) {
            aplicarTransferenciaAprobada(origen, destino, BigDecimal.valueOf(transaccion.getMonto()), transaccion);
        } else {
            log.info("Transaccion no aprobada (estado={}). Saldos no se actualizan.", estadoNombre);
        }

        String tipoNombre = transaccion.getTipoTransaccion() != null
                ? transaccion.getTipoTransaccion().getNombre() : null;
        transaccion.setTipoTransaccion(getTipo(tipoNombre));
        transaccion.setFechaCreacion(LocalDateTime.now());

        Transaccion resultado = transaccionRepository.save(transaccion);
        log.info("Transaccion guardada con ID: {}, Estado: {}",
                resultado.getId(), resultado.getEstadoNombre());
        return resultado;
    }

    public List<Transaccion> obtenerHistorial(String cuentaId) {
        try {
            List<Transaccion> enviadas  = transaccionRepository.findByCuentaOrigenId(cuentaId);
            List<Transaccion> recibidas = transaccionRepository.findByCuentaDestinoId(cuentaId);
            List<Transaccion> historial = new ArrayList<>();
            historial.addAll(enviadas);
            historial.addAll(recibidas);
            historial.sort(Comparator.comparing(Transaccion::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return historial;
        } catch (Exception e) {
            log.error("Error al obtener historial: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Transaccion> obtenerTodasTransacciones() {
        try {
            List<Transaccion> transacciones = transaccionRepository.findAll();
            transacciones.sort(Comparator.comparing(Transaccion::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return transacciones;
        } catch (Exception e) {
            log.error("Error al obtener todas las transacciones: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Transaccion> obtenerTransaccionesPendientes() {
        try {
            List<Transaccion> pendientes =
                    transaccionRepository.findByEstadoTransaccionNombre(ESTADO_PENDIENTE);
            pendientes.sort(Comparator.comparing(Transaccion::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return pendientes;
        } catch (Exception e) {
            log.error("Error al obtener transacciones pendientes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Transaccion actualizarEstadoTransaccion(Integer id, String nuevoEstadoNombre) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaccion no encontrada"));

        validarTransicionEstado(transaccion, nuevoEstadoNombre);

        if (ESTADO_APROBADA.equals(nuevoEstadoNombre)) {
            ejecutarTransferencia(transaccion);
        }

        transaccion.setEstadoTransaccion(getEstado(nuevoEstadoNombre));
        Transaccion actualizada = transaccionRepository.save(transaccion);
        log.info("Transaccion actualizada: id={}, estado={}", id, nuevoEstadoNombre);
        return actualizada;
    }
}
