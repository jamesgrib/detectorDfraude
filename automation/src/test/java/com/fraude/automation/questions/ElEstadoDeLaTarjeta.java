package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the card status from the last response.
 * Handles two response shapes:
 *   - Direct card object:  { "estadoNombre": "ACTIVA" }
 *   - Wrapped card object: { "tarjeta": { "estadoNombre": "ACTIVA" } }
 *
 * Usage:
 *   actor.asksAbout(ElEstadoDeLaTarjeta.enLaRespuesta())  → "ACTIVA"
 */
public class ElEstadoDeLaTarjeta implements Question<String> {

    private ElEstadoDeLaTarjeta() {}

    public static ElEstadoDeLaTarjeta enLaRespuesta() {
        return new ElEstadoDeLaTarjeta();
    }

    @Override
    public String answeredBy(Actor actor) {
        // Try wrapped shape first (POST /api/tarjetas returns { tarjeta: { estadoNombre } })
        String wrapped = SerenityRest.lastResponse().jsonPath().getString("tarjeta.estadoNombre");
        if (wrapped != null) {
            return wrapped;
        }
        // Fall back to direct shape
        return SerenityRest.lastResponse().jsonPath().getString("estadoNombre");
    }
}
