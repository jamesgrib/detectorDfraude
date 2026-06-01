package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the HTTP status code of the last REST response.
 *
 * Usage:
 *   actor.asksAbout(ElCodigoDRespuesta.actual())  → 200
 */
public class ElCodigoDRespuesta implements Question<Integer> {

    private ElCodigoDRespuesta() {}

    public static ElCodigoDRespuesta actual() {
        return new ElCodigoDRespuesta();
    }

    @Override
    public Integer answeredBy(Actor actor) {
        return SerenityRest.lastResponse().statusCode();
    }
}
