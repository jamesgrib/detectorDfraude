package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPUT;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

import java.util.Map;

/**
 * Business-level task: admin approves a pending transfer.
 * Calls PUT /api/transacciones/{id}/estado with estadoNombre=APROBADA.
 *
 * Usage:
 *   admin.attemptsTo(AprobarTransferencia.conId(5).comoAdmin("ADMIN001"));
 */
public class AprobarTransferencia implements Task {

    private final Integer transaccionId;
    private String adminDocumento;

    private AprobarTransferencia(Integer transaccionId) {
        this.transaccionId = transaccionId;
    }

    public static AprobarTransferencia conId(Integer transaccionId) {
        return new AprobarTransferencia(transaccionId);
    }

    public AprobarTransferencia comoAdmin(String adminDocumento) {
        this.adminDocumento = adminDocumento;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                EnviarPUT.a("/api/transacciones/" + transaccionId + "/estado")
                        .conHeader("X-Admin-Documento", adminDocumento)
                        .conCuerpo(Map.of("estadoNombre", "APROBADA")),
                CapturarRespuesta.actual()
        );
    }
}
