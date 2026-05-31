package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Business-level task: admin approves a pending card request.
 * Calls POST /api/tarjetas/{id}/aprobar.
 * For CREDITO cards, a credit limit can be specified.
 *
 * Usage:
 *   admin.attemptsTo(AprobarTarjeta.conId(3).yLimiteCredito(2000000.0));
 *   admin.attemptsTo(AprobarTarjeta.conId(3));  // DEBITO — no limit needed
 */
public class AprobarTarjeta implements Task {

    private final Integer tarjetaId;
    private Double limiteCredito;

    private AprobarTarjeta(Integer tarjetaId) {
        this.tarjetaId = tarjetaId;
    }

    public static AprobarTarjeta conId(Integer tarjetaId) {
        return new AprobarTarjeta(tarjetaId);
    }

    public AprobarTarjeta yLimiteCredito(Double limiteCredito) {
        this.limiteCredito = limiteCredito;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        Map<String, Object> body = new HashMap<>();
        if (limiteCredito != null && limiteCredito > 0) {
            body.put("limiteCredito", limiteCredito);
        }

        actor.attemptsTo(
                EnviarPOST.a("/api/tarjetas/" + tarjetaId + "/aprobar").conCuerpo(body),
                CapturarRespuesta.actual()
        );
    }
}
