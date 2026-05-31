package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPUT;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

import java.util.Map;

/**
 * Business-level task: admin rejects a pending transfer.
 * Calls PUT /api/transacciones/{id}/estado with estadoNombre=RECHAZADA.
 *
 * Usage:
 *   admin.attemptsTo(RechazarTransferencia.conId(5).comoAdmin("ADMIN001"));
 */
public class RechazarTransferencia implements Task {

    private final Integer transaccionId;
    private String adminDocumento;

    private RechazarTransferencia(Integer transaccionId) {
        this.transaccionId = transaccionId;
    }

    public static RechazarTransferencia conId(Integer transaccionId) {
        return new RechazarTransferencia(transaccionId);
    }

    public RechazarTransferencia comoAdmin(String adminDocumento) {
        this.adminDocumento = adminDocumento;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                EnviarPUT.a("/api/transacciones/" + transaccionId + "/estado")
                        .conHeader("X-Admin-Documento", adminDocumento)
                        .conCuerpo(Map.of("estadoNombre", "RECHAZADA")),
                CapturarRespuesta.actual()
        );
    }
}
