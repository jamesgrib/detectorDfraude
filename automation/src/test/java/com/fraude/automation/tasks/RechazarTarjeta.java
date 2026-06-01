package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Map;

public class RechazarTarjeta implements Performable {

    private final Integer tarjetaId;
    private final String motivo;

    private RechazarTarjeta(Integer tarjetaId, String motivo) {
        this.tarjetaId = tarjetaId;
        this.motivo = motivo;
    }

    public static RechazarTarjeta conId(Integer tarjetaId) {
        return new RechazarTarjeta(tarjetaId, null);
    }

    public RechazarTarjeta porMotivo(String motivo) {
        return new RechazarTarjeta(this.tarjetaId, motivo);
    }

    @Override
    @Step("{0} rejects card #tarjetaId")
    public <T extends Actor> void performAs(T actor) {
        Map<String, Object> body = motivo != null ? Map.of("motivo", motivo) : Map.of();
        EnviarPOST.a("/api/tarjetas/" + tarjetaId + "/rechazar").conCuerpo(body).performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
