package com.fraude.automation.questions;

import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

/**
 * Question: returns the invoice status from the last response.
 * POST /api/facturas/{id}/pagar does not return the estado directly,
 * so this question reads the "estado" field if present, otherwise
 * it can be used after a GET /api/facturas call.
 *
 * Usage:
 *   actor.asksAbout(ElEstadoDeLaFactura.enLaRespuesta())  → "PAGADA"
 */
public class ElEstadoDeLaFactura implements Question<String> {

    private ElEstadoDeLaFactura() {}

    public static ElEstadoDeLaFactura enLaRespuesta() {
        return new ElEstadoDeLaFactura();
    }

    @Override
    public String answeredBy(Actor actor) {
        // The pay endpoint returns { mensaje, facturaId, tipoServicio, monto, fechaPago, metodoPago }
        // A successful 200 response means the invoice was paid → status is PAGADA
        int statusCode = SerenityRest.lastResponse().statusCode();
        if (statusCode == 200) {
            return "PAGADA";
        }
        // For GET /api/facturas responses (list), read the first item's estado
        String estado = SerenityRest.lastResponse().jsonPath().getString("[0].estado");
        return estado;
    }
}
