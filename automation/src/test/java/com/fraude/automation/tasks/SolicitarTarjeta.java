package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.interactions.GuardarIdDesdeRespuesta;
import com.fraude.automation.model.TarjetaRequest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

/**
 * Business-level task: user requests a new card (DEBITO or CREDITO).
 * Calls POST /api/tarjetas with X-User-Documento header.
 * Stores the new card ID in actor memory under "tarjetaId".
 *
 * Usage:
 *   actor.attemptsTo(SolicitarTarjeta.deTipo("DEBITO").conTitular("Juan Perez").paraDocumento("12345678"));
 */
public class SolicitarTarjeta implements Task {

    private final String tipoTarjeta;
    private String nombreTitular;
    private String numDocumento;

    private SolicitarTarjeta(String tipoTarjeta) {
        this.tipoTarjeta = tipoTarjeta;
    }

    public static SolicitarTarjeta deTipo(String tipoTarjeta) {
        return new SolicitarTarjeta(tipoTarjeta);
    }

    public SolicitarTarjeta conTitular(String nombreTitular) {
        this.nombreTitular = nombreTitular;
        return this;
    }

    public SolicitarTarjeta paraDocumento(String numDocumento) {
        this.numDocumento = numDocumento;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        TarjetaRequest body = TarjetaRequest.builder()
                .tipoTarjeta(tipoTarjeta)
                .nombreTitular(nombreTitular)
                .build();

        actor.attemptsTo(
                EnviarPOST.a("/api/tarjetas")
                        .conHeader("X-User-Documento", numDocumento)
                        .conCuerpo(body),
                GuardarIdDesdeRespuesta.enNota("tarjetaId").desdeElCampo("tarjeta.id"),
                CapturarRespuesta.actual()
        );
    }
}
