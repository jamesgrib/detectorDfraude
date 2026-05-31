package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.model.TransferenciaRequest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

/**
 * Business-level task: transfer money between two accounts.
 * Calls POST /api/transacciones.
 *
 * Usage:
 *   actor.attemptsTo(RealizarTransferencia.de("ACC-001").a("ACC-002").porMonto(100000.0));
 */
public class RealizarTransferencia implements Task {

    private final String cuentaOrigen;
    private String cuentaDestino;
    private Double monto;

    private RealizarTransferencia(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public static RealizarTransferencia de(String cuentaOrigen) {
        return new RealizarTransferencia(cuentaOrigen);
    }

    public RealizarTransferencia a(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
        return this;
    }

    public RealizarTransferencia porMonto(Double monto) {
        this.monto = monto;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        TransferenciaRequest body = TransferenciaRequest.builder()
                .monto(monto)
                .cuentaOrigenId(cuentaOrigen)
                .cuentaDestinoId(cuentaDestino)
                .build();

        actor.attemptsTo(
                EnviarPOST.a("/api/transacciones").conCuerpo(body),
                CapturarRespuesta.actual()
        );
    }
}
