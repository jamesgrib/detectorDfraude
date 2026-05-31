package com.fraude.automation.tasks;

import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.model.LoginRequest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.annotations.Step;

/**
 * Business-level task: log in with document number and password.
 * Calls POST /api/usuarios/login and captures the response.
 */
public class IniciarSesion implements Performable {

    private final String numDocumento;
    private final String password;

    private IniciarSesion(String numDocumento, String password) {
        this.numDocumento = numDocumento;
        this.password = password;
    }

    public static IniciarSesion conDocumento(String numDocumento) {
        return new IniciarSesion(numDocumento, null);
    }

    public IniciarSesion yPassword(String password) {
        return new IniciarSesion(this.numDocumento, password);
    }

    @Override
    @Step("{0} logs in as #numDocumento")
    public <T extends Actor> void performAs(T actor) {
        LoginRequest body = LoginRequest.builder()
                .numDocumento(numDocumento)
                .password(password)
                .build();

        EnviarPOST.a("/api/usuarios/login").conCuerpo(body).performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
