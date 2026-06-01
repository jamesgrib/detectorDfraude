package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the "mensaje" or "message" field from the last response body.
 * Falls back to "error" if neither is present.
 *
 * Usage:
 *   actor.asksAbout(ElMensajeDeLaRespuesta.actual())  → "Login exitoso"
 */
public class ElMensajeDeLaRespuesta implements Question<String> {

    private ElMensajeDeLaRespuesta() {}

    public static ElMensajeDeLaRespuesta actual() {
        return new ElMensajeDeLaRespuesta();
    }

    @Override
    public String answeredBy(Actor actor) {
        String mensaje = SerenityRest.lastResponse().jsonPath().getString("mensaje");
        if (mensaje != null) return mensaje;

        String message = SerenityRest.lastResponse().jsonPath().getString("message");
        if (message != null) return message;

        return SerenityRest.lastResponse().jsonPath().getString("error");
    }
}
