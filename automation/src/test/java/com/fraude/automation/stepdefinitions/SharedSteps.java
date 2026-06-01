package com.fraude.automation.stepdefinitions;

import com.fraude.automation.questions.ElCampoDeLaRespuesta;
import com.fraude.automation.questions.ElCodigoDRespuesta;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import net.serenitybdd.rest.SerenityRest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable step definitions shared across all features.
 * Uses SerenityRest.lastResponse() directly to avoid dependency on OnStage.
 */
public class SharedSteps {

    @Then("the response status code is {int}")
    public void theResponseStatusCodeIs(int expectedCode) {
        assertThat(SerenityRest.lastResponse().statusCode())
                .as("HTTP status code")
                .isEqualTo(expectedCode);
    }

    @And("the response contains field {string} with value {string}")
    public void theResponseContainsFieldWithValue(String field, String expectedValue) {
        String actual = SerenityRest.lastResponse().jsonPath().getString(field);
        assertThat(actual)
                .as("Field '%s' in response", field)
                .isEqualTo(expectedValue);
    }

    @And("the response contains field {string}")
    public void theResponseContainsField(String field) {
        String actual = SerenityRest.lastResponse().jsonPath().getString(field);
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
