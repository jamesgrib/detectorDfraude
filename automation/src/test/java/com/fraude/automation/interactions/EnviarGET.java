package com.fraude.automation.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Get;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic GET interaction.
 *
 * Usage:
 *   actor.attemptsTo(EnviarGET.a("/api/tarjetas").conHeader("X-User-Documento", doc));
 */
public class EnviarGET implements Interaction {

    private final String path;
    private final Map<String, String> headers = new HashMap<>();

    private EnviarGET(String path) {
        this.path = path;
    }

    public static EnviarGET a(String path) {
        return new EnviarGET(path);
    }

    public EnviarGET conHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Get.resource(path).with(req -> {
                    headers.forEach(req::header);
                    return req;
                })
        );
    }
}
