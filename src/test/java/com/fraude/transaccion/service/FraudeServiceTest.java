package com.fraude.transaccion.service;

import com.fraude.transaccion.model.Transaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FraudeServiceTest {

    private FraudeService fraudeService;

    @BeforeEach
    void setUp() {
        fraudeService = new FraudeService();
    }

    @Test
    void evaluarFraude_montoNulo_retornaRechazada() {
        Transaccion t = new Transaccion();
        t.setMonto(null);

        String resultado = fraudeService.evaluarFraude(t);

        assertThat(resultado).isEqualTo("RECHAZADA");
    }

    @Test
    void evaluarFraude_montoBajo_retornaAprobada() {
        Transaccion t = new Transaccion();
        t.setMonto(100.0);

        String resultado = fraudeService.evaluarFraude(t);

        assertThat(resultado).isEqualTo("APROBADA");
    }

    @Test
    void evaluarFraude_montoExacto5Millones_retornaAprobada() {
        Transaccion t = new Transaccion();
        t.setMonto(5_000_000.0);

        String resultado = fraudeService.evaluarFraude(t);

        assertThat(resultado).isEqualTo("APROBADA");
    }

    @Test
    void evaluarFraude_montoSuperior5Millones_retornaPendiente() {
        Transaccion t = new Transaccion();
        t.setMonto(5_000_001.0);

        String resultado = fraudeService.evaluarFraude(t);

        assertThat(resultado).isEqualTo("PENDIENTE");
    }

    @Test
    void evaluarFraude_montoMuyAlto_retornaPendiente() {
        Transaccion t = new Transaccion();
        t.setMonto(10_000_000.0);

        String resultado = fraudeService.evaluarFraude(t);

        assertThat(resultado).isEqualTo("PENDIENTE");
    }

    @Test
    void evaluarFraude_montoCero_retornaAprobada() {
        Transaccion t = new Transaccion();
        t.setMonto(0.0);

        String resultado = fraudeService.evaluarFraude(t);

        assertThat(resultado).isEqualTo("APROBADA");
    }
}
