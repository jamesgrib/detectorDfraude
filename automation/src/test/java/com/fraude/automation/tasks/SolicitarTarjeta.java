package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.interactions.GuardarIdDesdeRespuesta;
import com.fraude.automation.model.TarjetaRequest;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

public class SolicitarTarjeta implements Performable {

    private final String tipoTarjeta;
    private final String nombreTitular;
    private final String numDocumento;

    private SolicitarTarjeta(String tipoTarjeta, String nombreTitular, String numDocumento) {
        this.tipoTarjeta = tipoTarjeta;
        this.nombreTitular = nombreTitular;
        this.numDocumento = numDocumento;
    }

    public static Builder deTipo(String tipoTarjeta) {
        return new Builder(tipoTarjeta);
    }

    public static class Builder {
        private final String tipo;
        private String titular;

        Builder(String tipo) { this.tipo = tipo; }

        public Builder conTitular(String titular) { this.titular = titular; return this; }

        public SolicitarTarjeta paraDocumento(String doc) {
            return new SolicitarTarjeta(tipo, titular, doc);
        }
    }

    @Override
    @Step("{0} requests a #tipoTarjeta card")
    public <T extends Actor> void performAs(T actor) {
        TarjetaRequest body = TarjetaRequest.builder()
                .tipoTarjeta(tipoTarjeta)
                .nombreTitular(nombreTitular)
                .build();

        EnviarPOST.a("/api/tarjetas")
                .conHeader("X-User-Documento", numDocumento)
                .conCuerpo(body)
                .performAs(actor);
        GuardarIdDesdeRespuesta.enNota("tarjetaId").desdeElCampo("tarjeta.id").performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
