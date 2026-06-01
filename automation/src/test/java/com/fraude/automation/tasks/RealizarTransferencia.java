package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.model.TransferenciaRequest;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

/**
 * Business-level task: transfer money between two accounts.
 * Calls POST /api/transacciones.
 */
public class RealizarTransferencia implements Performable {

    private final String cuentaOrigen;
    private final String cuentaDestino;
    private final Double monto;

    private RealizarTransferencia(String cuentaOrigen, String cuentaDestino, Double monto) {
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
    }

    public static Builder de(String cuentaOrigen) {
        return new Builder(cuentaOrigen);
    }

    public static class Builder {
        private final String origen;
        private String destino;

        Builder(String origen) { this.origen = origen; }

        public Builder a(String destino) { this.destino = destino; return this; }

        public RealizarTransferencia porMonto(Double monto) {
            return new RealizarTransferencia(origen, destino, monto);
        }
    }

    @Override
    @Step("{0} transfers #monto from #cuentaOrigen to #cuentaDestino")
    public <T extends Actor> void performAs(T actor) {
        TransferenciaRequest body = TransferenciaRequest.builder()
                .monto(monto)
                .cuentaOrigenId(cuentaOrigen)
                .cuentaDestinoId(cuentaDestino)
                .build();

        EnviarPOST.a("/api/transacciones").conCuerpo(body).performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
