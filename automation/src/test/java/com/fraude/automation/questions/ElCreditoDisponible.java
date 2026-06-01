package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the available credit from the last card response.
 * Handles both wrapped { tarjeta: { creditoDisponible } } and direct shapes.
 *
 * Usage:
 *   actor.asksAbout(ElCreditoDisponible.enLaRespuesta())  → 2000000.0
 */
public class ElCreditoDisponible implements Question<Double> {

    private ElCreditoDisponible() {}

    public static ElCreditoDisponible enLaRespuesta() {
        return new ElCreditoDisponible();
    }

    @Override
    public Double answeredBy(Actor actor) {
        Double wrapped = SerenityRest.lastResponse().jsonPath().getDouble("tarjeta.creditoDisponible");
        if (wrapped != null) {
            return wrapped;
        }
        return SerenityRest.lastResponse().jsonPath().getDouble("creditoDisponible");
    }
}
