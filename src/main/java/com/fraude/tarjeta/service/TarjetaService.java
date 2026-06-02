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
import java.security.SecureRandom;
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

    private final Random rnd = new SecureRandom();

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
        String numGenerado = generarNumeroTarjeta();
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
        requireTrue(ESTADO_PENDIENTE.equals(tarjeta.getEstadoNombre()),
                "La tarjeta no está pendiente de aprobación");
        tarjeta.setEstadoTarjeta(getEstado(ESTADO_ACTIVA));
        configurarLimitesSegunTipo(tarjeta, limiteCredito);
        tarjetaRepository.save(tarjeta);
        log.info("Tarjeta {} aprobada. Tipo: {}", tarjetaId, tarjeta.getTipoTarjeta());
        return tarjeta;
    }

    private double resolverLimiteCredito(Double limiteCredito) {
        return limiteCredito != null && limiteCredito > 0 ? limiteCredito : 1_000_000.0;
    }

    private void configurarLimitesSegunTipo(Tarjeta tarjeta, Double limiteCredito) {
        if ("CREDITO".equals(tarjeta.getTipoTarjeta())) {
            double limite = resolverLimiteCredito(limiteCredito);
            tarjeta.setLimiteCredito(limite);
            tarjeta.setCreditoDisponible(limite);
        } else {
            tarjeta.setSaldoTarjeta(0.0);
        }
    }

    public Tarjeta rechazarTarjeta(Integer tarjetaId, String motivo) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));
        requireTrue(ESTADO_PENDIENTE.equals(tarjeta.getEstadoNombre()),
                "La tarjeta no está pendiente de aprobación");
        tarjeta.setEstadoTarjeta(getEstado(ESTADO_RECHAZADA));
        tarjeta.setMotivoRechazo(motivo != null ? motivo : "Solicitud rechazada por el administrador");
        tarjetaRepository.save(tarjeta);
        log.info("Tarjeta {} rechazada", tarjetaId);
        return tarjeta;
    }

    public Tarjeta recargarDebito(Integer tarjetaId, String numDocumento, Double monto, String numeroCuenta) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));

        validarMonto(monto);
        validarTarjetaParaRecarga(tarjeta, numDocumento);

        Cuenta cuenta = cuentaRepository.findById(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta bancaria no encontrada"));

        validarCuentaParaRecarga(cuenta, numDocumento, BigDecimal.valueOf(monto));

        cuenta.setSaldo(cuenta.getSaldo().subtract(BigDecimal.valueOf(monto)));
        cuentaRepository.save(cuenta);

        double saldoActual = tarjeta.getSaldoTarjeta() != null ? tarjeta.getSaldoTarjeta() : 0.0;
        tarjeta.setSaldoTarjeta(saldoActual + monto);
        tarjetaRepository.save(tarjeta);

        log.info("Recarga de {} a tarjeta débito {}. Nuevo saldo tarjeta: {}",
                monto, tarjetaId, tarjeta.getSaldoTarjeta());
        return tarjeta;
    }

    private void validarMonto(Double monto) {
        requireTrue(monto != null && monto > 0, "El monto debe ser mayor a 0");
    }

    private void validarTarjetaParaRecarga(Tarjeta tarjeta, String numDocumento) {
        requireTrue(tarjeta.getNumDocumento().equals(numDocumento),
                "La tarjeta no pertenece al usuario");
        requireTrue(ESTADO_ACTIVA.equals(tarjeta.getEstadoNombre()),
                "La tarjeta no está activa");
        requireTrue("DEBITO".equals(tarjeta.getTipoTarjeta()),
                "Solo se pueden recargar tarjetas de débito");
    }

    private void requireTrue(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private void validarCuentaParaRecarga(Cuenta cuenta, String numDocumento, BigDecimal monto) {
        requireTrue(cuenta.getNumDocumento().equals(numDocumento),
                "La cuenta no pertenece al usuario");
        requireTrue(cuenta.getSaldo().compareTo(monto) >= 0,
                "Saldo insuficiente en la cuenta bancaria. Saldo: $" + cuenta.getSaldo());
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

    private void eliminarTarjetaValidada(Tarjeta tarjeta, String numDocumento) {
        requireTrue(tarjeta.getNumDocumento().equals(numDocumento),
                "No tienes permiso para eliminar esta tarjeta");
    }

    public void eliminarTarjeta(Integer tarjetaId, String numDocumento) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException(TARJETA_NO_ENCONTRADA));
        eliminarTarjetaValidada(tarjeta, numDocumento);
        tarjeta.setEstadoTarjeta(getEstado(ESTADO_ELIMINADA));
        tarjetaRepository.save(tarjeta);
        log.info("Tarjeta desactivada: {}", tarjetaId);
    }

    private String generarNumeroTarjeta() {
        StringBuilder sb = new StringBuilder();
        sb.append(rnd.nextBoolean() ? "4" : "5" + (1 + rnd.nextInt(5)));
        while (sb.length() < 16) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }

    private String detectarMarca(String numero) {
        if (numero == null || numero.isEmpty()) return "UNKNOWN";
        return resolverMarcaNoNulo(numero);
    }

    private static final java.util.Map<Character, String> MARCA_POR_PREFIJO = java.util.Map.of(
            '4', "VISA"
    );

    private String resolverMarcaNoNulo(String numero) {
        char first = numero.charAt(0);
        if (MARCA_POR_PREFIJO.containsKey(first)) return MARCA_POR_PREFIJO.get(first);
        if (esMastercard(numero, first)) return "MASTERCARD";
        if (esAmex(numero, first)) return "AMEX";
        return "UNKNOWN";
    }

    private boolean esMastercard(String numero, char first) {
        return esMastercardPrefijo5(numero, first) || esMastercardPrefijo2(numero, first);
    }

    private boolean esMastercardPrefijo5(String numero, char first) {
        if (first != '5' || numero.length() <= 1) return false;
        char second = numero.charAt(1);
        return second >= '1' && second <= '5';
    }

    private boolean esMastercardPrefijo2(String numero, char first) {
        if (first != '2' || numero.length() < 4) return false;
        return esRangoMastercardPrefijo2(numero.substring(0, 4));
    }

    private boolean esRangoMastercardPrefijo2(String prefix4) {
        try {
            int prefix = Integer.parseInt(prefix4);
            return prefix >= 2221 && prefix <= 2720;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean esAmex(String numero, char first) {
        if (first != '3' || numero.length() <= 1) return false;
        char second = numero.charAt(1);
        return second == '4' || second == '7';
    }
}
