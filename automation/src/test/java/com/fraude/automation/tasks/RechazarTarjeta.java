package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

import java.util.Map;

/**
 * Business-level task: admin rejects a pending card request.
 * Calls POST /api/tarjetas/{id}/rechazar with an optional rejection reason.
 *
 * Usage:
 *   admin.attemptsTo(RechazarTarjeta.conId(3).porMotivo("Documentación incompleta"));
 */
public class RechazarTarjeta implements Task {

    private final Integer tarjetaId;
    private String motivo;

    private RechazarTarjeta(Integer tarjetaId) {
        this.tarjetaId = tarjetaId;
    }

    public static RechazarTarjeta conId(Integer tarjetaId) {
        return new RechazarTarjeta(tarjetaId);
    }

    public RechazarTarjeta porMotivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        Map<String, Object> body = motivo != null
                ? Map.of("motivo", motivo)
                : Map.of();

        actor.attemptsTo(
                EnviarPOST.a("/api/tarjetas/" + tarjetaId + "/rechazar").conCuerpo(body),
                CapturarRespuesta.actual()
        );
    }
}
