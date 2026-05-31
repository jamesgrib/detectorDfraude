package com.fraude.automation.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.rest.SerenityRest;

/**
 * Extracts an integer ID from the last REST response JSON body
 * and stores it in the actor's memory under the given key.
 *
 * Usage:
 *   actor.attemptsTo(GuardarIdDesdeRespuesta.enNota("tarjetaId").desdeElCampo("tarjeta.id"));
 */
public class GuardarIdDesdeRespuesta implements Interaction {

    private final String noteKey;
    private String jsonPath;

    private GuardarIdDesdeRespuesta(String noteKey) {
        this.noteKey = noteKey;
    }

    public static GuardarIdDesdeRespuesta enNota(String noteKey) {
        return new GuardarIdDesdeRespuesta(noteKey);
    }

    public GuardarIdDesdeRespuesta desdeElCampo(String jsonPath) {
        this.jsonPath = jsonPath;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        Object value = SerenityRest.lastResponse().jsonPath().get(jsonPath);
        actor.remember(noteKey, value);
    }
}
