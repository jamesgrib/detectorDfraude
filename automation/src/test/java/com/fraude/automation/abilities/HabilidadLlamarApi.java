package com.fraude.automation.abilities;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Ability;

/**
 * Ability that grants an actor the power to call the BancoDigital REST API.
 * The base URL is resolved from serenity.conf → environments.default.api.base.url
 */
public class HabilidadLlamarApi implements Ability {

    private final String baseUrl;

    private HabilidadLlamarApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Factory method.
     * Usage: actor.can(HabilidadLlamarApi.conUrlBase("http://localhost:8080"))
     */
    public static HabilidadLlamarApi conUrlBase(String baseUrl) {
        return new HabilidadLlamarApi(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Retrieves this ability from the actor's context.
     */
    public static HabilidadLlamarApi como(Actor actor) {
        return actor.abilityTo(HabilidadLlamarApi.class);
    }

    @Override
    public String toString() {
        return "call the BancoDigital API at " + baseUrl;
    }
}
