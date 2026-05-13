package com.fraude.factura.controller;

import com.fraude.factura.model.EstadoFactura;
import com.fraude.factura.model.Factura;
import com.fraude.factura.model.Servicio;
import com.fraude.factura.service.FacturaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaControllerTest {

    @Mock private FacturaService facturaService;

    @InjectMocks
    private FacturaController controller;

    private Factura buildFactura() {
        return Factura.builder()
                .id(1)
                .numDocumento("12345678")
                .servicio(Servicio.builder().id(1).nombre("LUZ").build())
                .monto(100_000.0)
                .estadoFactura(EstadoFactura.builder().id(2).nombre("PAGADA").build())
                .referencia("REF-001")
                .fechaCreacion(LocalDateTime.now())
                .fechaVencimiento(LocalDateTime.now().plusDays(15))
                .fechaPago(LocalDateTime.now())
                .build();
    }

    // ─── obtenerFacturas ──────────────────────────────────────────────────────

    @Test
    void obtenerFacturas_retornaLista() {
        when(facturaService.obtenerFacturas("12345678")).thenReturn(List.of(buildFactura()));

        ResponseEntity<?> response = controller.obtenerFacturas("12345678");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Factura> body = (List<Factura>) response.getBody();
        assertThat(body).hasSize(1);
    }

    // ─── generarFacturasPrueba ────────────────────────────────────────────────

    @Test
    void generarFacturasPrueba_retorna200() {
        when(facturaService.generarFacturasPrueba("12345678")).thenReturn(List.of(buildFactura()));

        ResponseEntity<?> response = controller.generarFacturasPrueba("12345678", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("mensaje")).isEqualTo("Facturas generadas");
    }

    // ─── pagarFactura ─────────────────────────────────────────────────────────

    @Test
    void pagarFactura_conTarjeta_exitoso() {
        when(facturaService.pagarFactura(1, "12345678", 1, null)).thenReturn(buildFactura());

        Map<String, Object> body = Map.of("tarjetaId", 1);
        ResponseEntity<?> response = controller.pagarFactura("12345678", 1, body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("mensaje")).isEqualTo("Factura pagada exitosamente");
        assertThat(resp.get("metodoPago")).isEqualTo("TARJETA");
    }

    @Test
    void pagarFactura_conCuenta_exitoso() {
        when(facturaService.pagarFactura(1, "12345678", null, "ACC-001")).thenReturn(buildFactura());

        Map<String, Object> body = Map.of("numeroCuenta", "ACC-001");
        ResponseEntity<?> response = controller.pagarFactura("12345678", 1, body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("metodoPago")).isEqualTo("SALDO");
    }

    @Test
    void pagarFactura_error_retorna400() {
        when(facturaService.pagarFactura(any(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("Saldo insuficiente"));

        ResponseEntity<?> response = controller.pagarFactura("12345678", 1, Map.of("numeroCuenta", "ACC-001"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("error")).isEqualTo("Saldo insuficiente");
    }
}
