package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the estadoNombre field from the last transaction response.
 * Reads directly from the last response body (no extra GET needed).
 *
 * Usage:
 *   actor.asksAbout(ElEstadoDeLaTransaccion.enLaRespuesta())  → "APROBADA"
 */
public class ElEstadoDeLaTransaccion implements Question<String> {

    private ElEstadoDeLaTransaccion() {}

    public static ElEstadoDeLaTransaccion enLaRespuesta() {
        return new ElEstadoDeLaTransaccion();
    }

    @Override
    public String answeredBy(Actor actor) {
        return SerenityRest.lastResponse().jsonPath().getString("estadoNombre");
    }
}
