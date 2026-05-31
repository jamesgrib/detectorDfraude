package com.fraude.automation.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Delete;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic DELETE interaction.
 *
 * Usage:
 *   actor.attemptsTo(EnviarDELETE.a("/api/tarjetas/3").conHeader("X-User-Documento", doc));
 */
public class EnviarDELETE implements Interaction {

    private final String path;
    private final Map<String, String> headers = new HashMap<>();

    private EnviarDELETE(String path) {
        this.path = path;
    }

    public static EnviarDELETE a(String path) {
        return new EnviarDELETE(path);
    }

    public EnviarDELETE conHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Delete.from(path).with(req -> {
                    headers.forEach(req::header);
                    return req;
                })
        );
    }
}
