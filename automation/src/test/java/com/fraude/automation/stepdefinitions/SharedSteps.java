package com.fraude.automation.stepdefinitions;

import com.fraude.automation.questions.ElCampoDeLaRespuesta;
import com.fraude.automation.questions.ElCodigoDRespuesta;
import com.fraude.automation.questions.ElMensajeDeLaRespuesta;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable step definitions shared across all features.
 * Covers generic HTTP status, field presence, and message assertions.
 */
public class SharedSteps {

    private Actor actor() {
        return OnStage.theActorInTheSpotlight();
    }

    @Then("the response status code is {int}")
    public void theResponseStatusCodeIs(int expectedCode) {
        assertThat(ElCodigoDRespuesta.actual().answeredBy(actor()))
                .as("HTTP status code")
                .isEqualTo(expectedCode);
    }

    @And("the response contains field {string} with value {string}")
    public void theResponseContainsFieldWithValue(String field, String expectedValue) {
        String actual = ElCampoDeLaRespuesta.llamado(field).answeredBy(actor());
        assertThat(actual)
                .as("Field '%s' in response", field)
                .isEqualTo(expectedValue);
    }

    @And("the response contains field {string}")
    public void theResponseContainsField(String field) {
        String actual = ElCampoDeLaRespuesta.llamado(field).answeredBy(actor());
        assertThat(actual)
                .as("Field '%s' should be present in response", field)
                .isNotNull();
    }

    @And("the response contains a non-empty list of {string}")
    public void theResponseContainsNonEmptyListOf(String field) {
        java.util.List<?> list = SerenityRest.lastResponse().jsonPath().getList(field);
        assertThat(list)
                .as("List '%s' in response", field)
                .isNotNull()
                .isNotEmpty();
    }

    @And("the error message contains {string}")
    public void theErrorMessageContains(String fragment) {
        String body = SerenityRest.lastResponse().asString();
        assertThat(body)
                .as("Response body should contain '%s'", fragment)
                .contains(fragment);
    }
}
