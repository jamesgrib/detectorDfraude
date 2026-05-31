package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPUT;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Map;

public class AprobarTransferencia implements Performable {

    private final Integer transaccionId;
    private final String adminDocumento;

    private AprobarTransferencia(Integer transaccionId, String adminDocumento) {
        this.transaccionId = transaccionId;
        this.adminDocumento = adminDocumento;
    }

    public static AprobarTransferencia conId(Integer id) {
        return new AprobarTransferencia(id, null);
    }

    public AprobarTransferencia comoAdmin(String adminDocumento) {
        return new AprobarTransferencia(this.transaccionId, adminDocumento);
    }

    @Override
    @Step("{0} approves transfer #transaccionId")
    public <T extends Actor> void performAs(T actor) {
        EnviarPUT.a("/api/transacciones/" + transaccionId + "/estado")
                .conHeader("X-Admin-Documento", adminDocumento)
                .conCuerpo(Map.of("estadoNombre", "APROBADA"))
                .performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
