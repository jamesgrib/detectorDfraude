package com.fraude.factura.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.factura.model.EstadoFactura;
import com.fraude.factura.model.Factura;
import com.fraude.factura.model.Servicio;
import com.fraude.factura.repository.EstadoFacturaRepository;
import com.fraude.factura.repository.FacturaRepository;
import com.fraude.factura.repository.ServicioRepository;
import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.repository.TarjetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaService {

    private final Random rnd = new Random();

    private final FacturaRepository facturaRepository;
    private final TarjetaRepository tarjetaRepository;
    private final CuentaRepository cuentaRepository;
    private final ServicioRepository servicioRepository;
    private final EstadoFacturaRepository estadoFacturaRepository;

    private Servicio getServicio(String nombre) {
        return servicioRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de servicio no encontrado: " + nombre));
    }

    private EstadoFactura getEstado(String nombre) {
        return estadoFacturaRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estado de factura no encontrado: " + nombre));
    }

    public List<Factura> generarFacturasPrueba(String numDocumento) {
        String[] servicios     = { "LUZ", "AGUA", "INTERNET", "GAS", "TELEFONO" };
        String[] descripciones = {
                "Electricidad - EPM Mes actual",
                "Acueducto y alcantarillado - Aguas Nacionales",
                "Plan Hogar 200 Mbps - Internet Plus",
                "Gas natural domiciliario - GasNatural",
                "Plan movil 10GB - TeleCel"
        };
        double[][] rangos = {
                { 60000, 180000 },
                { 30000, 90000  },
                { 50000, 120000 },
                { 25000, 75000  },
                { 40000, 100000 }
        };

        EstadoFactura estadoPendiente = getEstado("PENDIENTE");

        for (int i = 0; i < servicios.length; i++) {
            crearFacturaSiNoExiste(numDocumento, servicios[i], descripciones[i], rangos[i], i, estadoPendiente);
        }
        return facturaRepository.findByNumDocumentoOrderByFechaVencimientoDesc(numDocumento);
    }

    private void crearFacturaSiNoExiste(String numDocumento, String servicioNombre,
            String descripcion, double[] rango, int diasExtra, EstadoFactura estadoPendiente) {
        List<Factura> existentes = facturaRepository
                .findByNumDocumentoAndEstadoFacturaNombre(numDocumento, "PENDIENTE");
        boolean yaExiste = existentes.stream()
                .anyMatch(f -> servicioNombre.equals(f.getTipoServicio()));

        if (yaExiste) {
            return;
        }

        long minCentenas = (long) (rango[0] / 100);
        long maxCentenas = (long) (rango[1] / 100);
        double montoAleatorio = (minCentenas + rnd.nextLong(maxCentenas - minCentenas + 1)) * 100.0;

        Factura factura = Factura.builder()
                .numDocumento(numDocumento)
                .servicio(getServicio(servicioNombre))
                .descripcion(descripcion)
                .referencia("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .monto(montoAleatorio)
                .estadoFactura(estadoPendiente)
                .fechaVencimiento(LocalDateTime.now().plusDays(15L + diasExtra * 3L))
                .fechaCreacion(LocalDateTime.now())
                .build();
        facturaRepository.save(factura);
    }

    public List<Factura> obtenerFacturas(String numDocumento) {
        return facturaRepository.findByNumDocumentoOrderByFechaVencimientoDesc(numDocumento);
    }

    public Factura pagarFactura(Integer facturaId, String numDocumento,
            Integer tarjetaId, String numeroCuenta) {

        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        validarPropietarioFactura(factura, numDocumento);
        validarFacturaNoPagada(factura);

        if (tarjetaId != null) {
            pagarConTarjeta(factura, numDocumento, tarjetaId);
        } else if (numeroCuenta != null) {
            pagarConCuenta(factura, numDocumento, numeroCuenta);
        } else {
            throw new IllegalArgumentException("Debes indicar una tarjeta o una cuenta para pagar");
        }

        factura.setEstadoFactura(getEstado("PAGADA"));
        factura.setFechaPago(LocalDateTime.now());
        facturaRepository.save(factura);

        log.info("Factura {} pagada exitosamente", facturaId);
        return factura;
    }

    private void validarPropietarioFactura(Factura factura, String numDocumento) {
        if (!factura.getNumDocumento().equals(numDocumento)) {
            throw new IllegalArgumentException("No tienes permiso para pagar esta factura");
        }
    }

    private void validarFacturaNoPagada(Factura factura) {
        if ("PAGADA".equals(factura.getEstado())) {
            throw new IllegalArgumentException("Esta factura ya fue pagada");
        }
    }

    private void pagarConTarjeta(Factura factura, String numDocumento, Integer tarjetaId) {
        Tarjeta tarjeta = tarjetaRepository.findById(tarjetaId)
                .orElseThrow(() -> new IllegalArgumentException("Tarjeta no encontrada"));

        if (!tarjeta.getNumDocumento().equals(numDocumento)) {
            throw new IllegalArgumentException("La tarjeta no pertenece al usuario");
        }
        if (!"ACTIVA".equals(tarjeta.getEstadoNombre())) {
            throw new IllegalArgumentException("La tarjeta no esta activa");
        }

        double monto = factura.getMonto();
        if ("CREDITO".equals(tarjeta.getTipoTarjeta())) {
            pagarConTarjetaCredito(tarjeta, monto);
        } else {
            pagarConTarjetaDebito(tarjeta, monto);
        }

        tarjetaRepository.save(tarjeta);
        factura.setTarjetaId(tarjetaId);
    }

    private void pagarConTarjetaCredito(Tarjeta tarjeta, double monto) {
        double disponible = tarjeta.getCreditoDisponible() != null ? tarjeta.getCreditoDisponible() : 0.0;
        if (disponible < monto) {
            throw new IllegalArgumentException(
                    "Credito disponible insuficiente. Disponible: $" + disponible + " | Factura: $" + monto);
        }
        tarjeta.setCreditoDisponible(disponible - monto);
        log.info("Pago con tarjeta credito. Credito restante: {}", tarjeta.getCreditoDisponible());
    }

    private void pagarConTarjetaDebito(Tarjeta tarjeta, double monto) {
        double saldo = tarjeta.getSaldoTarjeta() != null ? tarjeta.getSaldoTarjeta() : 0.0;
        if (saldo < monto) {
            throw new IllegalArgumentException(
                    "Saldo de tarjeta debito insuficiente. Saldo: $" + saldo + " | Factura: $" + monto);
        }
        tarjeta.setSaldoTarjeta(saldo - monto);
        log.info("Pago con tarjeta debito. Saldo restante: {}", tarjeta.getSaldoTarjeta());
    }

    private void pagarConCuenta(Factura factura, String numDocumento, String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findById(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));

        if (!cuenta.getNumDocumento().equals(numDocumento)) {
            throw new IllegalArgumentException("La cuenta no pertenece al usuario");
        }
        if (cuenta.getSaldo().compareTo(BigDecimal.valueOf(factura.getMonto())) < 0) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente. Saldo: $" + cuenta.getSaldo() + " | Factura: $" + factura.getMonto());
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(BigDecimal.valueOf(factura.getMonto())));
        cuentaRepository.save(cuenta);
        log.info("Factura pagada con saldo de cuenta. Nuevo saldo: {}", cuenta.getSaldo());
    }
}
