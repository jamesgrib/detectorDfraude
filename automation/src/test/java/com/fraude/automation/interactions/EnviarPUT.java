package com.fraude.automation.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Put;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic PUT interaction.
 *
 * Usage:
 *   actor.attemptsTo(EnviarPUT.a("/api/transacciones/5/estado").conCuerpo(body).conHeader("X-Admin-Documento", doc));
 */
public class EnviarPUT implements Interaction {

    private final String path;
    private Object body;
    private final Map<String, String> headers = new HashMap<>();

    private EnviarPUT(String path) {
        this.path = path;
    }

    public static EnviarPUT a(String path) {
        return new EnviarPUT(path);
    }

    public EnviarPUT conCuerpo(Object body) {
        this.body = body;
        return this;
    }

    public EnviarPUT conHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Put.to(path).with(req -> {
                    req.contentType("application/json");
                    headers.forEach(req::header);
                    if (body != null) {
                        req.body(body);
                    }
                    return req;
                })
        );
    }
}
