package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

/**
 * Business-level task: generate test invoices for a user.
 * Calls POST /api/facturas/generar-prueba with X-User-Documento header.
 *
 * Usage:
 *   actor.attemptsTo(GenerarFacturasDePrueba.paraDocumento("12345678"));
 */
public class GenerarFacturasDePrueba implements Task {

    private final String numDocumento;

    private GenerarFacturasDePrueba(String numDocumento) {
        this.numDocumento = numDocumento;
    }

    public static GenerarFacturasDePrueba paraDocumento(String numDocumento) {
        return new GenerarFacturasDePrueba(numDocumento);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                EnviarPOST.a("/api/facturas/generar-prueba")
                        .conHeader("X-User-Documento", numDocumento),
                CapturarRespuesta.actual()
        );
    }
}
