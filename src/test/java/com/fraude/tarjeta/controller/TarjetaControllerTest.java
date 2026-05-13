package com.fraude.tarjeta.controller;

import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.service.TarjetaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarjetaControllerTest {

    @Mock private TarjetaService tarjetaService;

    @InjectMocks
    private TarjetaController controller;

    private Tarjeta buildTarjeta() {
        return Tarjeta.builder()
                .id(1)
                .numDocumento("12345678")
                .tipoTarjeta("DEBITO")
                .ultimosCuatro("1234")
                .nombreTitular("Juan Pérez")
                .fechaExpiracion("05/2030")
                .marcaTarjeta(MarcaTarjeta.builder().id(1).nombre("VISA").build())
                .estadoTarjeta(EstadoTarjeta.builder().id(1).nombre("ACTIVA").build())
                .limiteCredito(0.0)
                .creditoDisponible(0.0)
                .saldoTarjeta(500_000.0)
                .build();
    }

    @Test
    void obtenerTarjetas_retornaLista() {
        when(tarjetaService.obtenerTarjetasUsuario("12345678")).thenReturn(List.of(buildTarjeta()));

        ResponseEntity<?> response = controller.obtenerTarjetas("12345678");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Tarjeta> body = (List<Tarjeta>) response.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void solicitarTarjeta_exitoso_retorna200() {
        when(tarjetaService.solicitarTarjeta(anyString(), anyString(), anyString()))
                .thenReturn(buildTarjeta());

        Map<String, Object> body = Map.of("nombreTitular", "Juan Pérez", "tipoTarjeta", "DEBITO");
        ResponseEntity<?> response = controller.solicitarTarjeta("12345678", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("mensaje")).isNotNull();
    }

    @Test
    void solicitarTarjeta_error_propagaExcepcion() {
        when(tarjetaService.solicitarTarjeta(anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("Error al solicitar"));

        assertThatThrownBy(() -> controller.solicitarTarjeta("12345678",
                Map.of("nombreTitular", "Juan", "tipoTarjeta", "DEBITO")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error al solicitar");
    }

    @Test
    void recargarDebito_exitoso_retorna200() {
        Tarjeta t = buildTarjeta();
        t.setSaldoTarjeta(600_000.0);
        when(tarjetaService.recargarDebito(1, "12345678", 100_000.0, "ACC-001")).thenReturn(t);

        Map<String, Object> body = Map.of("monto", 100_000.0, "numeroCuenta", "ACC-001");
        ResponseEntity<?> response = controller.recargarDebito("12345678", 1, body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("mensaje")).isEqualTo("Recarga exitosa");
    }

    @Test
    void recargarDebito_error_propagaExcepcion() {
        when(tarjetaService.recargarDebito(any(), anyString(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("Saldo insuficiente"));

        assertThatThrownBy(() -> controller.recargarDebito("12345678", 1,
                Map.of("monto", 999_999_999.0, "numeroCuenta", "ACC-001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void obtenerPendientes_retornaLista() {
        when(tarjetaService.obtenerPendientes()).thenReturn(List.of(buildTarjeta()));

        ResponseEntity<?> response = controller.obtenerPendientes();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void obtenerTodas_retornaLista() {
        when(tarjetaService.obtenerTodas()).thenReturn(List.of(buildTarjeta(), buildTarjeta()));

        ResponseEntity<?> response = controller.obtenerTodas();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void aprobarTarjeta_exitoso_retorna200() {
        Tarjeta aprobada = buildTarjeta();
        aprobada.setEstadoTarjeta(EstadoTarjeta.builder().id(2).nombre("ACTIVA").build());
        when(tarjetaService.aprobarTarjeta(1, 1_000_000.0)).thenReturn(aprobada);

        ResponseEntity<?> response = controller.aprobarTarjeta(1, Map.of("limiteCredito", 1_000_000.0));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("mensaje")).isEqualTo("Tarjeta aprobada exitosamente");
    }

    @Test
    void aprobarTarjeta_sinBody_usaLimiteNulo() {
        when(tarjetaService.aprobarTarjeta(1, null)).thenReturn(buildTarjeta());

        ResponseEntity<?> response = controller.aprobarTarjeta(1, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void aprobarTarjeta_error_propagaExcepcion() {
        when(tarjetaService.aprobarTarjeta(99, null))
                .thenThrow(new IllegalArgumentException("Tarjeta no encontrada"));

        assertThatThrownBy(() -> controller.aprobarTarjeta(99, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tarjeta no encontrada");
    }

    @Test
    void rechazarTarjeta_exitoso_retorna200() {
        Tarjeta rechazada = buildTarjeta();
        rechazada.setEstadoTarjeta(EstadoTarjeta.builder().id(3).nombre("RECHAZADA").build());
        when(tarjetaService.rechazarTarjeta(1, "Documentación incompleta")).thenReturn(rechazada);

        ResponseEntity<?> response = controller.rechazarTarjeta(1, Map.of("motivo", "Documentación incompleta"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("mensaje")).isEqualTo("Tarjeta rechazada");
    }

    @Test
    void rechazarTarjeta_sinBody_usaMotivoNulo() {
        when(tarjetaService.rechazarTarjeta(1, null)).thenReturn(buildTarjeta());

        ResponseEntity<?> response = controller.rechazarTarjeta(1, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void rechazarTarjeta_error_propagaExcepcion() {
        doThrow(new IllegalArgumentException("Tarjeta no encontrada"))
                .when(tarjetaService).rechazarTarjeta(eq(99), any());

        assertThatThrownBy(() -> controller.rechazarTarjeta(99, Map.of("motivo", "test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tarjeta no encontrada");
    }

    @Test
    void eliminarTarjeta_exitoso_retorna200() {
        ResponseEntity<?> response = controller.eliminarTarjeta("12345678", 1);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) response.getBody();
        assertThat(resp.get("mensaje")).isEqualTo("Tarjeta eliminada exitosamente");
    }

    @Test
    void eliminarTarjeta_error_propagaExcepcion() {
        doThrow(new IllegalArgumentException("No tienes permiso"))
                .when(tarjetaService).eliminarTarjeta(99, "12345678");

        assertThatThrownBy(() -> controller.eliminarTarjeta("12345678", 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permiso");
    }
}
