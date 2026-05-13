package com.fraude.factura.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.factura.model.EstadoFactura;
import com.fraude.factura.model.Factura;
import com.fraude.factura.model.Servicio;
import com.fraude.factura.repository.EstadoFacturaRepository;
import com.fraude.factura.repository.FacturaRepository;
import com.fraude.factura.repository.ServicioRepository;
import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.repository.TarjetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FacturaServiceTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private TarjetaRepository tarjetaRepository;
    @Mock private CuentaRepository cuentaRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EstadoFacturaRepository estadoFacturaRepository;

    @InjectMocks
    private FacturaService facturaService;

    private EstadoFactura estadoPendiente;
    private EstadoFactura estadoPagada;
    private Servicio servicioLuz;
    private Factura facturaPendiente;
    private Tarjeta tarjetaCredito;
    private Tarjeta tarjetaDebito;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        estadoPendiente = EstadoFactura.builder().id(1).nombre("PENDIENTE").build();
        estadoPagada = EstadoFactura.builder().id(2).nombre("PAGADA").build();
        servicioLuz = Servicio.builder().id(1).nombre("LUZ").build();

        facturaPendiente = Factura.builder()
                .id(1)
                .numDocumento("12345678")
                .servicio(servicioLuz)
                .monto(100_000.0)
                .estadoFactura(estadoPendiente)
                .referencia("REF-001")
                .fechaCreacion(LocalDateTime.now())
                .fechaVencimiento(LocalDateTime.now().plusDays(15))
                .build();

        EstadoTarjeta estadoActiva = EstadoTarjeta.builder().id(1).nombre("ACTIVA").build();

        tarjetaCredito = Tarjeta.builder()
                .id(1)
                .numDocumento("12345678")
                .tipoTarjeta("CREDITO")
                .estadoTarjeta(estadoActiva)
                .limiteCredito(500_000.0)
                .creditoDisponible(500_000.0)
                .build();

        tarjetaDebito = Tarjeta.builder()
                .id(2)
                .numDocumento("12345678")
                .tipoTarjeta("DEBITO")
                .estadoTarjeta(estadoActiva)
                .saldoTarjeta(500_000.0)
                .build();

        cuenta = Cuenta.builder()
                .numeroCuenta("ACC-001")
                .saldo(BigDecimal.valueOf(500_000))
                .numDocumento("12345678")
                .build();
    }

    // ─── obtenerFacturas ──────────────────────────────────────────────────────

    @Test
    void obtenerFacturas_retornaListaDelUsuario() {
        when(facturaRepository.findByNumDocumentoOrderByFechaVencimientoDesc("12345678"))
                .thenReturn(List.of(facturaPendiente));

        List<Factura> result = facturaService.obtenerFacturas("12345678");

        assertThat(result).hasSize(1);
        verify(facturaRepository).findByNumDocumentoOrderByFechaVencimientoDesc("12345678");
    }

    // ─── pagarFactura — validaciones ──────────────────────────────────────────

    @Test
    void pagarFactura_facturaNoExiste_lanzaExcepcion() {
        when(facturaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.pagarFactura(99, "12345678", null, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void pagarFactura_noPertenece_lanzaExcepcion() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "99999999", null, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    void pagarFactura_yaFuePagada_lanzaExcepcion() {
        Factura facturaPagada = Factura.builder()
                .id(2).numDocumento("12345678").monto(50_000.0)
                .estadoFactura(estadoPagada).referencia("REF-002")
                .build();
        when(facturaRepository.findById(2)).thenReturn(Optional.of(facturaPagada));

        assertThatThrownBy(() -> facturaService.pagarFactura(2, "12345678", null, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya fue pagada");
    }

    @Test
    void pagarFactura_sinMetodoPago_lanzaExcepcion() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tarjeta o una cuenta");
    }

    // ─── pagarFactura — con tarjeta de crédito ────────────────────────────────

    @Test
    void pagarFactura_tarjetaNoExiste_lanzaExcepcion() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(tarjetaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", 99, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tarjeta no encontrada");
    }

    @Test
    void pagarFactura_tarjetaNoPertenece_lanzaExcepcion() {
        Tarjeta tarjetaAjena = Tarjeta.builder().id(1).numDocumento("99999999")
                .tipoTarjeta("CREDITO").estadoTarjeta(EstadoTarjeta.builder().id(1).nombre("ACTIVA").build())
                .creditoDisponible(500_000.0).build();
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjetaAjena));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", 1, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no pertenece al usuario");
    }

    @Test
    void pagarFactura_creditoInsuficiente_lanzaExcepcion() {
        Tarjeta tarjetaPobre = Tarjeta.builder().id(1).numDocumento("12345678")
                .tipoTarjeta("CREDITO").estadoTarjeta(EstadoTarjeta.builder().id(1).nombre("ACTIVA").build())
                .creditoDisponible(50_000.0).build();
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjetaPobre));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", 1, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credito disponible insuficiente");
    }

    @Test
    void pagarFactura_conTarjetaCredito_exitoso() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjetaCredito));
        when(estadoFacturaRepository.findByNombre("PAGADA")).thenReturn(Optional.of(estadoPagada));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Factura result = facturaService.pagarFactura(1, "12345678", 1, null);

        assertThat(result.getEstado()).isEqualTo("PAGADA");
        assertThat(tarjetaCredito.getCreditoDisponible()).isEqualTo(400_000.0);
        verify(tarjetaRepository).save(tarjetaCredito);
        verify(facturaRepository).save(facturaPendiente);
    }

    // ─── pagarFactura — con tarjeta de débito ─────────────────────────────────

    @Test
    void pagarFactura_debitoInsuficiente_lanzaExcepcion() {
        Tarjeta tarjetaPobre = Tarjeta.builder().id(2).numDocumento("12345678")
                .tipoTarjeta("DEBITO").estadoTarjeta(EstadoTarjeta.builder().id(1).nombre("ACTIVA").build())
                .saldoTarjeta(50_000.0).build();
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(tarjetaRepository.findById(2)).thenReturn(Optional.of(tarjetaPobre));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", 2, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Saldo de tarjeta debito insuficiente");
    }

    @Test
    void pagarFactura_conTarjetaDebito_exitoso() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(tarjetaRepository.findById(2)).thenReturn(Optional.of(tarjetaDebito));
        when(estadoFacturaRepository.findByNombre("PAGADA")).thenReturn(Optional.of(estadoPagada));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Factura result = facturaService.pagarFactura(1, "12345678", 2, null);

        assertThat(result.getEstado()).isEqualTo("PAGADA");
        assertThat(tarjetaDebito.getSaldoTarjeta()).isEqualTo(400_000.0);
    }

    // ─── pagarFactura — con cuenta bancaria ───────────────────────────────────

    @Test
    void pagarFactura_cuentaNoExiste_lanzaExcepcion() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(cuentaRepository.findById("ACC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", null, "ACC-999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cuenta no encontrada");
    }

    @Test
    void pagarFactura_cuentaNoPertenece_lanzaExcepcion() {
        Cuenta cuentaAjena = Cuenta.builder().numeroCuenta("ACC-001")
                .saldo(BigDecimal.valueOf(500_000)).numDocumento("99999999").build();
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuentaAjena));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", null, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no pertenece al usuario");
    }

    @Test
    void pagarFactura_saldoCuentaInsuficiente_lanzaExcepcion() {
        Cuenta cuentaPobre = Cuenta.builder().numeroCuenta("ACC-001")
                .saldo(BigDecimal.valueOf(50_000)).numDocumento("12345678").build();
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuentaPobre));

        assertThatThrownBy(() -> facturaService.pagarFactura(1, "12345678", null, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void pagarFactura_conCuenta_exitoso() {
        when(facturaRepository.findById(1)).thenReturn(Optional.of(facturaPendiente));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuenta));
        when(estadoFacturaRepository.findByNombre("PAGADA")).thenReturn(Optional.of(estadoPagada));
        when(cuentaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Factura result = facturaService.pagarFactura(1, "12345678", null, "ACC-001");

        assertThat(result.getEstado()).isEqualTo("PAGADA");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(400_000));
        verify(cuentaRepository).save(cuenta);
    }

    // ─── generarFacturasPrueba ────────────────────────────────────────────────

    @Test
    void generarFacturasPrueba_creaFacturasCuandoNoExisten() {
        when(facturaRepository.findByNumDocumentoAndEstadoFacturaNombre(anyString(), anyString()))
                .thenReturn(List.of());
        when(servicioRepository.findByNombre(anyString())).thenReturn(Optional.of(servicioLuz));
        when(estadoFacturaRepository.findByNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(facturaRepository.findByNumDocumentoOrderByFechaVencimientoDesc("12345678"))
                .thenReturn(List.of(facturaPendiente));

        List<Factura> result = facturaService.generarFacturasPrueba("12345678");

        assertThat(result).isNotEmpty();
        verify(facturaRepository, times(5)).save(any(Factura.class));
    }

    @Test
    void generarFacturasPrueba_noCreaDuplicados() {
        // El mock retorna una factura con nombre "LUZ" para todos los servicios,
        // así el check yaExiste siempre es true y nunca guarda
        Factura facturaLuz = Factura.builder()
                .id(1).numDocumento("12345678")
                .servicio(Servicio.builder().id(1).nombre("LUZ").build())
                .estadoFactura(estadoPendiente).monto(100_000.0).referencia("REF-001").build();
        Factura facturaAgua = Factura.builder()
                .id(2).numDocumento("12345678")
                .servicio(Servicio.builder().id(2).nombre("AGUA").build())
                .estadoFactura(estadoPendiente).monto(50_000.0).referencia("REF-002").build();
        Factura facturaInternet = Factura.builder()
                .id(3).numDocumento("12345678")
                .servicio(Servicio.builder().id(3).nombre("INTERNET").build())
                .estadoFactura(estadoPendiente).monto(60_000.0).referencia("REF-003").build();
        Factura facturaGas = Factura.builder()
                .id(4).numDocumento("12345678")
                .servicio(Servicio.builder().id(4).nombre("GAS").build())
                .estadoFactura(estadoPendiente).monto(40_000.0).referencia("REF-004").build();
        Factura facturaTelefono = Factura.builder()
                .id(5).numDocumento("12345678")
                .servicio(Servicio.builder().id(5).nombre("TELEFONO").build())
                .estadoFactura(estadoPendiente).monto(70_000.0).referencia("REF-005").build();

        List<Factura> todasExistentes = List.of(facturaLuz, facturaAgua, facturaInternet, facturaGas, facturaTelefono);

        when(facturaRepository.findByNumDocumentoAndEstadoFacturaNombre(anyString(), anyString()))
                .thenReturn(todasExistentes);
        when(estadoFacturaRepository.findByNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(facturaRepository.findByNumDocumentoOrderByFechaVencimientoDesc("12345678"))
                .thenReturn(todasExistentes);

        facturaService.generarFacturasPrueba("12345678");

        verify(facturaRepository, never()).save(any(Factura.class));
    }
}
