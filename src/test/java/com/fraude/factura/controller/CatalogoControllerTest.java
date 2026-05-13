package com.fraude.factura.controller;

import com.fraude.factura.model.EstadoFactura;
import com.fraude.factura.model.Servicio;
import com.fraude.factura.repository.EstadoFacturaRepository;
import com.fraude.factura.repository.ServicioRepository;
import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.repository.EstadoTarjetaRepository;
import com.fraude.tarjeta.repository.MarcaTarjetaRepository;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.TipoTransaccion;
import com.fraude.transaccion.repository.EstadoTransaccionRepository;
import com.fraude.transaccion.repository.TipoTransaccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock private ServicioRepository servicioRepository;
    @Mock private EstadoFacturaRepository estadoFacturaRepository;
    @Mock private MarcaTarjetaRepository marcaTarjetaRepository;
    @Mock private TipoTransaccionRepository tipoTransaccionRepository;
    @Mock private EstadoTarjetaRepository estadoTarjetaRepository;
    @Mock private EstadoTransaccionRepository estadoTransaccionRepository;

    @InjectMocks
    private CatalogoController controller;

    @Test
    void getServicios_retornaLista() {
        when(servicioRepository.findAll()).thenReturn(List.of(
                Servicio.builder().id(1).nombre("LUZ").build(),
                Servicio.builder().id(2).nombre("AGUA").build()
        ));

        ResponseEntity<List<Servicio>> response = controller.getServicios();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getEstadosFactura_retornaLista() {
        when(estadoFacturaRepository.findAll()).thenReturn(List.of(
                EstadoFactura.builder().id(1).nombre("PENDIENTE").build(),
                EstadoFactura.builder().id(2).nombre("PAGADA").build()
        ));

        ResponseEntity<List<EstadoFactura>> response = controller.getEstadosFactura();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getMarcasTarjeta_retornaLista() {
        when(marcaTarjetaRepository.findAll()).thenReturn(List.of(
                MarcaTarjeta.builder().id(1).nombre("VISA").build(),
                MarcaTarjeta.builder().id(2).nombre("MASTERCARD").build()
        ));

        ResponseEntity<List<MarcaTarjeta>> response = controller.getMarcasTarjeta();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getTiposTransaccion_retornaLista() {
        when(tipoTransaccionRepository.findAll()).thenReturn(List.of(
                TipoTransaccion.builder().id(1).nombre("TRANSFERENCIA").build(),
                TipoTransaccion.builder().id(2).nombre("DEPOSITO").build()
        ));

        ResponseEntity<List<TipoTransaccion>> response = controller.getTiposTransaccion();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getEstadosTarjeta_retornaLista() {
        when(estadoTarjetaRepository.findAll()).thenReturn(List.of(
                EstadoTarjeta.builder().id(1).nombre("ACTIVA").build(),
                EstadoTarjeta.builder().id(2).nombre("PENDIENTE").build()
        ));

        ResponseEntity<List<EstadoTarjeta>> response = controller.getEstadosTarjeta();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getEstadosTransaccion_retornaLista() {
        when(estadoTransaccionRepository.findAll()).thenReturn(List.of(
                EstadoTransaccion.builder().id(1).nombre("APROBADA").build(),
                EstadoTransaccion.builder().id(2).nombre("RECHAZADA").build(),
                EstadoTransaccion.builder().id(3).nombre("PENDIENTE").build()
        ));

        ResponseEntity<List<EstadoTransaccion>> response = controller.getEstadosTransaccion();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(3);
    }
}
