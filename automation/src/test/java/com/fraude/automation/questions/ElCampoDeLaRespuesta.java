package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the value of a JSON field from the last REST response.
 *
 * Usage:
 *   actor.asksAbout(ElCampoDeLaRespuesta.llamado("success"))   → "true"
 *   actor.asksAbout(ElCampoDeLaRespuesta.llamado("estadoNombre")) → "APROBADA"
 */
public class ElCampoDeLaRespuesta implements Question<String> {

    private final String jsonPath;

    private ElCampoDeLaRespuesta(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public static ElCampoDeLaRespuesta llamado(String jsonPath) {
        return new ElCampoDeLaRespuesta(jsonPath);
    }

    @Override
    public String answeredBy(Actor actor) {
        Object value = SerenityRest.lastResponse().jsonPath().get(jsonPath);
        return value != null ? value.toString() : null;
    }
}
