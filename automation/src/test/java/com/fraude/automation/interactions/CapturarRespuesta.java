package com.fraude.automation.interactions;

import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;

/**
 * Stores the last REST Assured response in the actor's memory
 * under the key "ultimaRespuesta", so Questions can retrieve it later.
 *
 * Usage:
 *   actor.attemptsTo(CapturarRespuesta.actual());
 */
public class CapturarRespuesta implements Interaction {

    /** Note key used to store the last response on the actor. */
    public static final String ULTIMA_RESPUESTA = "ultimaRespuesta";

    private CapturarRespuesta() {}

    public static CapturarRespuesta actual() {
        return new CapturarRespuesta();
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        Response response = SerenityRest.lastResponse();
        actor.remember(ULTIMA_RESPUESTA, response);
    }
}
