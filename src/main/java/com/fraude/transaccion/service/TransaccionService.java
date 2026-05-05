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
                        .orElseThrow(() -> new RuntimeException("Tipo de transacción no encontrado: " + key)));
    }

    private EstadoTransaccion getEstado(String nombre) {
        return estadoTransaccionRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Estado de transacción no encontrado: " + nombre));
    }

    @Transactional
    public Transaccion procesarTransaccion(Transaccion transaccion) {
        try {
            log.info("Procesando transacción: origen={}, destino={}, monto={}",
                    transaccion.getCuentaOrigenId(),
                    transaccion.getCuentaDestinoId(),
                    transaccion.getMonto());

            if (transaccion.getMonto() == null || transaccion.getMonto() <= 0) {
                throw new IllegalArgumentException("Monto debe ser mayor a 0");
            }
            if (transaccion.getCuentaOrigenId() == null || transaccion.getCuentaOrigenId().isEmpty()) {
                throw new IllegalArgumentException("Cuenta origen es requerida");
            }
            if (transaccion.getCuentaDestinoId() == null || transaccion.getCuentaDestinoId().isEmpty()) {
                throw new IllegalArgumentException("Cuenta destino es requerida");
            }

            Cuenta origen = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
            Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));

            // Evaluar fraude (devuelve nombre: APROBADA, PENDIENTE, RECHAZADA)
            String estadoNombre = fraudeService.evaluarFraude(transaccion);
            log.info("Estado de fraude evaluado: {}", estadoNombre);
            transaccion.setEstadoTransaccion(getEstado(estadoNombre));

            // Si es aprobada, actualizar saldos
            if ("APROBADA".equals(estadoNombre)) {
                BigDecimal montoTransferencia = BigDecimal.valueOf(transaccion.getMonto());
                if (origen.getSaldo().compareTo(montoTransferencia) < 0) {
                    log.error("Saldo insuficiente: disponible={}, requerido={}",
                            origen.getSaldo(), montoTransferencia);
                    transaccion.setEstadoTransaccion(getEstado("RECHAZADA"));
                } else {
                    origen.setSaldo(origen.getSaldo().subtract(montoTransferencia));
                    destino.setSaldo(destino.getSaldo().add(montoTransferencia));
                    cuentaRepository.save(origen);
                    cuentaRepository.save(destino);
                    log.info("Saldos actualizados. Origen: {}, Destino: {}",
                            origen.getSaldo(), destino.getSaldo());
                }
            } else {
                log.info("ℹTransacción no aprobada (estado={}). Saldos no se actualizan.", estadoNombre);
            }

            // Asignar tipo y fecha
            if (transaccion.getTipoTransaccion() == null) {
                transaccion.setTipoTransaccion(getTipo(null));
            } else {
                transaccion.setTipoTransaccion(getTipo(transaccion.getTipoTransaccion().getNombre()));
            }
            transaccion.setFechaCreacion(LocalDateTime.now());

            Transaccion resultado = transaccionRepository.save(transaccion);
            log.info("Transacción guardada con ID: {}, Estado: {}", resultado.getId(), resultado.getEstadoNombre());
            return resultado;

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al procesar transacción: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar transacción: " + e.getMessage());
        }
    }

    public List<Transaccion> obtenerHistorial(String cuentaId) {
        try {
            List<Transaccion> enviadas = transaccionRepository.findByCuentaOrigenId(cuentaId);
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
            List<Transaccion> pendientes = transaccionRepository.findByEstadoTransaccionNombre("PENDIENTE");
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
        try {
            Transaccion transaccion = transaccionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

            String estadoActual = transaccion.getEstadoNombre();

            if (!"PENDIENTE".equals(estadoActual)) {
                throw new IllegalArgumentException("Solo se pueden validar transacciones en estado PENDIENTE");
            }

            if (!"APROBADA".equals(nuevoEstadoNombre) && !"RECHAZADA".equals(nuevoEstadoNombre)) {
                throw new IllegalArgumentException("Estado inválido. Debe ser APROBADA o RECHAZADA");
            }

            // Si cambia de PENDIENTE a APROBADA, actualizar saldos
            if ("APROBADA".equals(nuevoEstadoNombre)) {
                Cuenta origen = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                        .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
                Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                        .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));

                BigDecimal montoTransferencia = BigDecimal.valueOf(transaccion.getMonto());
                if (origen.getSaldo().compareTo(montoTransferencia) < 0) {
                    throw new IllegalArgumentException("Saldo insuficiente en la cuenta origen");
                }

                origen.setSaldo(origen.getSaldo().subtract(montoTransferencia));
                destino.setSaldo(destino.getSaldo().add(montoTransferencia));
                cuentaRepository.save(origen);
                cuentaRepository.save(destino);
            }

            transaccion.setEstadoTransaccion(getEstado(nuevoEstadoNombre));
            Transaccion actualizada = transaccionRepository.save(transaccion);
            log.info("Transacción actualizada: id={}, estado={}", id, nuevoEstadoNombre);
            return actualizada;

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al actualizar estado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar estado: " + e.getMessage());
        }
    }
}
