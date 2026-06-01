package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPUT;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Map;

public class RechazarTransferencia implements Performable {

    private final Integer transaccionId;
    private final String adminDocumento;

    private RechazarTransferencia(Integer transaccionId, String adminDocumento) {
        this.transaccionId = transaccionId;
        this.adminDocumento = adminDocumento;
    }

    public static RechazarTransferencia conId(Integer id) {
        return new RechazarTransferencia(id, null);
    }

    public RechazarTransferencia comoAdmin(String adminDocumento) {
        return new RechazarTransferencia(this.transaccionId, adminDocumento);
    }

    @Override
    @Step("{0} rejects transfer #transaccionId")
    public <T extends Actor> void performAs(T actor) {
        EnviarPUT.a("/api/transacciones/" + transaccionId + "/estado")
                .conHeader("X-Admin-Documento", adminDocumento)
                .conCuerpo(Map.of("estadoNombre", "RECHAZADA"))
                .performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
