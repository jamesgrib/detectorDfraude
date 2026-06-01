package com.fraude.automation.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic POST interaction.
 * Delegates to Serenity's built-in Post interaction, adding
 * optional JSON body and custom headers.
 *
 * Usage:
 *   actor.attemptsTo(EnviarPOST.a("/api/usuarios/login").conCuerpo(body));
 *   actor.attemptsTo(EnviarPOST.a("/api/tarjetas").conHeader("X-User-Documento", doc).conCuerpo(body));
 */
public class EnviarPOST implements Interaction {

    private final String path;
    private Object body;
    private final Map<String, String> headers = new HashMap<>();

    private EnviarPOST(String path) {
        this.path = path;
    }

    public static EnviarPOST a(String path) {
        return new EnviarPOST(path);
    }

    public EnviarPOST conCuerpo(Object body) {
        this.body = body;
        return this;
    }

    public EnviarPOST conHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Post.to(path).with(req -> {
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
