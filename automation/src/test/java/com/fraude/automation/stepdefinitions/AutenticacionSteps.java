package com.fraude.automation.stepdefinitions;

import com.fraude.automation.actors.ActorFactory;
import com.fraude.automation.tasks.IniciarSesion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for autenticacion.feature.
 */
public class AutenticacionSteps {

    private Actor actor;

    @Given("a user attempts to login with document {string} and password {string}")
    public void aUserAttemptsToLoginWithDocumentAndPassword(String document, String password) {
        OnStage.setTheStage(new OnlineCast());
        actor = ActorFactory.usuarioBancario(document, "Usuario " + document);
        IniciarSesion.conDocumento(document).yPassword(password).performAs(actor);
    }

    @Then("the login response code should be {int}")
    public void theLoginResponseCodeShouldBe(int expectedCode) {
        assertThat(SerenityRest.lastResponse().statusCode())
                .as("HTTP status code")
                .isEqualTo(expectedCode);
    }

    @And("the login success flag should be {word}")
    public void theLoginSuccessFlagShouldBe(String expectedSuccess) {
        String actual = SerenityRest.lastResponse().jsonPath().getString("success");
        assertThat(actual)
                .as("Login success flag")
                .isEqualTo(expectedSuccess);
    }

    @Given("a registered user with document {string} and password {string}")
    public void aRegisteredUserWithDocumentAndPassword(String document, String password) {
        OnStage.setTheStage(new OnlineCast());
        actor = ActorFactory.usuarioBancario(document, "Usuario " + document);
        actor.remember("password", password);
    }

    @When("the user logs in")
    public void theUserLogsIn() {
        String document = actor.recall(ActorFactory.NUM_DOCUMENTO);
        String password = actor.recall("password");
        IniciarSesion.conDocumento(document).yPassword(password).performAs(actor);
    }
}
