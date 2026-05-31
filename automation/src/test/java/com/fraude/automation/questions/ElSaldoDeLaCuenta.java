package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the account balance from the last login/register response.
 * The login response includes { saldo: 500000.00 }.
 *
 * Usage:
 *   actor.asksAbout(ElSaldoDeLaCuenta.enLaRespuesta())  → 500000.0
 */
public class ElSaldoDeLaCuenta implements Question<Double> {

    private ElSaldoDeLaCuenta() {}

    public static ElSaldoDeLaCuenta enLaRespuesta() {
        return new ElSaldoDeLaCuenta();
    }

    @Override
    public Double answeredBy(Actor actor) {
        return SerenityRest.lastResponse().jsonPath().getDouble("saldo");
    }
}
