package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.model.LoginRequest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

/**
 * Business-level task: log in with document number and password.
 * Calls POST /api/usuarios/login and captures the response.
 *
 * Usage:
 *   actor.attemptsTo(IniciarSesion.conDocumento("12345678").yPassword("pass123"));
 */
public class IniciarSesion implements Task {

    private final String numDocumento;
    private String password;

    private IniciarSesion(String numDocumento) {
        this.numDocumento = numDocumento;
    }

    public static IniciarSesion conDocumento(String numDocumento) {
        return new IniciarSesion(numDocumento);
    }

    public IniciarSesion yPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        LoginRequest body = LoginRequest.builder()
                .numDocumento(numDocumento)
                .password(password)
                .build();

        actor.attemptsTo(
                EnviarPOST.a("/api/usuarios/login").conCuerpo(body),
                CapturarRespuesta.actual()
        );
    }
}
