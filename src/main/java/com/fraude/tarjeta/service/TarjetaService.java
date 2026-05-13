package com.fraude.tarjeta.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.repository.EstadoTarjetaRepository;
import com.fraude.tarjeta.repository.MarcaTarjetaRepository;
import com.fraude.tarjeta.repository.TarjetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarjetaService {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_ACTIVA    = "ACTIVA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_ELIMINADA = "ELIMINADA";
    private static final String TARJETA_NO_ENCONTRADA = "Tarjeta no encontrada";

    // Instancia reutilizable de Random
    private final Random rnd = new Random();

    private final TarjetaRepository tarjetaRepository;
    private final CuentaRepository cuentaRepository;
    private final MarcaTarjetaRepository marcaTarjetaRepository;
    private final EstadoTarjetaRepository estadoTarjetaRepository;

    private MarcaTarjeta getMarca(String nombre) {
        return marcaTarjetaRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Marca de tarjeta no encontrada: " + nombre));
    }

    private EstadoTarjeta getEstado(String nombre) {
        return estadoTarjetaRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Estado de tarjeta no encontrado: " + nombre));
    }

    public Tarjeta solicitarTarjeta(String numDocumento, String nombreTitular, String tipoTarjeta) {
        boolean esVisa = rnd.nextBoolean();
        StringBuilder sb = new StringBuilder();
        if (esVisa) {
            sb.append("4");
        } else {
            sb.append("5").append(1 + rnd.nextInt(5));
        }
        while (sb.length() < 16) {
            sb.append(rnd.nextInt(10));
        }
        String numGenerado = sb.toString();
        String marca    = detectarMarca(numGenerado);
        String ultimos4 = numGenerado.substring(12);

        LocalDateTime expDate = LocalDateTime.now().plusYears(4);
        String fechaExp = String.format("%02d/%d", expDate.getMonthValue(), expDate.getYear());

        Tarjeta tarjeta = Tarjeta.builder()
                .numDocumento(numDocumento)
                .tipoTarjeta(tipoTarjeta)
                .ultimosCuatro(ultimos4)
                .nombreTitular(nombreTitular)
                .fechaExpiracion(fechaExp)
                .marcaTarjeta(getMarca(marca))
                .estadoTarjeta(getEstado(ESTADO_PENDIENTE))
                .limiteCredito(0.0)
                .creditoDisponible(0.0)
                .saldoTarjeta(0.0)
                .fechaCreacion(LocalDateTime.now())
                .build();

        tarjetaRepository.save(tarjeta);
        log.info("Solicitud de tarjeta registrada: marca={}, ultimos4={}, exp={}, estado=PENDIENTE",
                marca, ultimos4, fechaExp);
        return tarjeta;
    }

    public Tarjeta aprobarTarjeta(Integer tarjetaId, Double limiteCredito) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));

        if (!ESTADO_PENDIENTE.equals(tarjeta.getEstadoNombre())) {
            throw new IllegalArgumentException("La tarjeta no está pendiente de aprobación");
        }

        tarjeta.setEstadoTarjeta(getEstado(ESTADO_ACTIVA));
        if ("CREDITO".equals(tarjeta.getTipoTarjeta())) {
            double limite = limiteCredito != null && limiteCredito > 0 ? limiteCredito : 1_000_000.0;
            tarjeta.setLimiteCredito(limite);
            tarjeta.setCreditoDisponible(limite);
        } else {
            tarjeta.setSaldoTarjeta(0.0);
        }

        tarjetaRepository.save(tarjeta);
        log.info("Tarjeta {} aprobada. Tipo: {}", tarjetaId, tarjeta.getTipoTarjeta());
        return tarjeta;
    }

    public Tarjeta rechazarTarjeta(Integer tarjetaId, String motivo) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));

        if (!ESTADO_PENDIENTE.equals(tarjeta.getEstadoNombre())) {
            throw new IllegalArgumentException("La tarjeta no está pendiente de aprobación");
        }

        tarjeta.setEstadoTarjeta(getEstado(ESTADO_RECHAZADA));
        tarjeta.setMotivoRechazo(motivo != null ? motivo : "Solicitud rechazada por el administrador");
        tarjetaRepository.save(tarjeta);
        log.info("Tarjeta {} rechazada", tarjetaId);
        return tarjeta;
    }

    public Tarjeta recargarDebito(Integer tarjetaId, String numDocumento, Double monto, String numeroCuenta) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));

        if (!tarjeta.getNumDocumento().equals(numDocumento)) {
            throw new IllegalArgumentException("La tarjeta no pertenece al usuario");
        }
        if (!ESTADO_ACTIVA.equals(tarjeta.getEstadoNombre())) {
            throw new IllegalArgumentException("La tarjeta no está activa");
        }
        if (!"DEBITO".equals(tarjeta.getTipoTarjeta())) {
            throw new IllegalArgumentException("Solo se pueden recargar tarjetas de débito");
        }
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        Cuenta cuenta = cuentaRepository.findById(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta bancaria no encontrada"));

        if (!cuenta.getNumDocumento().equals(numDocumento)) {
            throw new IllegalArgumentException("La cuenta no pertenece al usuario");
        }
        if (cuenta.getSaldo().compareTo(BigDecimal.valueOf(monto)) < 0) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente en la cuenta bancaria. Saldo: $" + cuenta.getSaldo());
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(BigDecimal.valueOf(monto)));
        cuentaRepository.save(cuenta);

        tarjeta.setSaldoTarjeta((tarjeta.getSaldoTarjeta() != null ? tarjeta.getSaldoTarjeta() : 0.0) + monto);
        tarjetaRepository.save(tarjeta);

        log.info("Recarga de {} a tarjeta débito {}. Nuevo saldo tarjeta: {}",
                monto, tarjetaId, tarjeta.getSaldoTarjeta());
        return tarjeta;
    }

    public List<Tarjeta> obtenerTarjetasUsuario(String numDocumento) {
        return tarjetaRepository.findByNumDocumento(numDocumento);
    }

    public List<Tarjeta> obtenerPendientes() {
        return tarjetaRepository.findByEstadoTarjetaNombre(ESTADO_PENDIENTE);
    }

    public List<Tarjeta> obtenerTodas() {
        return tarjetaRepository.findAllByOrderByFechaCreacionDesc();
    }

    public void eliminarTarjeta(Integer tarjetaId, String numDocumento) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));

        if (!tarjeta.getNumDocumento().equals(numDocumento)) {
            throw new IllegalArgumentException("No tienes permiso para eliminar esta tarjeta");
        }

        tarjeta.setEstadoTarjeta(getEstado(ESTADO_ELIMINADA));
        tarjetaRepository.save(tarjeta);
        log.info("Tarjeta desactivada: {}", tarjetaId);
    }

    private String detectarMarca(String numero) {
        if (numero == null || numero.isEmpty()) return "UNKNOWN";
        char first = numero.charAt(0);
        if (first == '4') return "VISA";
        if (esMastercardPrefijo5(numero, first)) return "MASTERCARD";
        if (esMastercardPrefijo2(numero, first)) return "MASTERCARD";
        if (esAmex(numero, first)) return "AMEX";
        return "UNKNOWN";
    }

    private boolean esMastercardPrefijo5(String numero, char first) {
        if (first != '5' || numero.length() <= 1) return false;
        char second = numero.charAt(1);
        return second >= '1' && second <= '5';
    }

    private boolean esMastercardPrefijo2(String numero, char first) {
        if (first != '2' || numero.length() < 4) return false;
        try {
            int prefix = Integer.parseInt(numero.substring(0, 4));
            return prefix >= 2221 && prefix <= 2720;
        } catch (NumberFormatException ignored) {
            // número no parseable
            return false;
        }
    }

    private boolean esAmex(String numero, char first) {
        if (first != '3' || numero.length() <= 1) return false;
        char second = numero.charAt(1);
        return second == '4' || second == '7';
    }
}
