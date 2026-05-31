package com.fraude.automation.actors;

import com.fraude.automation.abilities.HabilidadLlamarApi;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;

/**
 * Factory that creates the two main actors in the BancoDigital domain:
 *
 *  - UsuarioBancario  → regular user identified by X-User-Documento header
 *  - AdministradorBancario → admin user identified by X-Admin-Documento header
 *
 * Both actors receive {@link HabilidadLlamarApi} (base URL) and
 * {@link CallAnApi} (Serenity REST integration) as abilities.
 *
 * The document number is stored as a named note so interactions and
 * questions can retrieve it without coupling to a global variable.
 */
public class ActorFactory {

    /** Note key used to store the user's document number on the actor. */
    public static final String NUM_DOCUMENTO = "numDocumento";

    private static final String BASE_URL =
            System.getProperty("api.base.url", "https://fraude-detection-backend.onrender.com");

    private ActorFactory() {
        // utility class — no instances
    }

    /**
     * Creates a regular banking user actor.
     *
     * @param numDocumento the user's document number (used as X-User-Documento header)
     * @param nombre       display name for Serenity reports
     */
    public static Actor usuarioBancario(String numDocumento, String nombre) {
        Actor actor = Actor.named(nombre)
                .whoCan(HabilidadLlamarApi.conUrlBase(BASE_URL))
                .whoCan(CallAnApi.at(BASE_URL));
        actor.remember(NUM_DOCUMENTO, numDocumento);
        return actor;
    }

    /**
     * Creates an administrator actor.
     * Admins use the X-Admin-Documento header instead of X-User-Documento.
     *
     * @param numDocumento the admin's document number
     * @param nombre       display name for Serenity reports
     */
    public static Actor administradorBancario(String numDocumento, String nombre) {
        Actor actor = Actor.named(nombre)
                .whoCan(HabilidadLlamarApi.conUrlBase(BASE_URL))
                .whoCan(CallAnApi.at(BASE_URL));
        actor.remember(NUM_DOCUMENTO, numDocumento);
        return actor;
    }
}
