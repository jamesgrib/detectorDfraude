package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.HashMap;
import java.util.Map;

public class AprobarTarjeta implements Performable {

    private final Integer tarjetaId;
    private final Double limiteCredito;

    private AprobarTarjeta(Integer tarjetaId, Double limiteCredito) {
        this.tarjetaId = tarjetaId;
        this.limiteCredito = limiteCredito;
    }

    public static AprobarTarjeta conId(Integer tarjetaId) {
        return new AprobarTarjeta(tarjetaId, null);
    }

    public AprobarTarjeta yLimiteCredito(Double limiteCredito) {
        return new AprobarTarjeta(this.tarjetaId, limiteCredito);
    }

    @Override
    @Step("{0} approves card #tarjetaId")
    public <T extends Actor> void performAs(T actor) {
        Map<String, Object> body = new HashMap<>();
        if (limiteCredito != null && limiteCredito > 0) {
            body.put("limiteCredito", limiteCredito);
        }
        EnviarPOST.a("/api/tarjetas/" + tarjetaId + "/aprobar").conCuerpo(body).performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
