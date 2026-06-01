package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

public class GenerarFacturasDePrueba implements Performable {

    private final String numDocumento;

    private GenerarFacturasDePrueba(String numDocumento) {
        this.numDocumento = numDocumento;
    }

    public static GenerarFacturasDePrueba paraDocumento(String numDocumento) {
        return new GenerarFacturasDePrueba(numDocumento);
    }

    @Override
    @Step("{0} generates test invoices")
    public <T extends Actor> void performAs(T actor) {
        EnviarPOST.a("/api/facturas/generar-prueba")
                .conHeader("X-User-Documento", numDocumento)
                .performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
