package com.fraude.automation.stepdefinitions;

import com.fraude.automation.actors.ActorFactory;
import com.fraude.automation.questions.ElCreditoDisponible;
import com.fraude.automation.questions.ElEstadoDeLaTarjeta;
import com.fraude.automation.tasks.AprobarTarjeta;
import com.fraude.automation.tasks.RechazarTarjeta;
import com.fraude.automation.tasks.SolicitarTarjeta;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for tarjetas.feature.
 */
public class TarjetaSteps {

    private static final String ADMIN_DOC = "12345678";

    private Actor usuario;
    private Actor admin;

    // ── Shared helpers ────────────────────────────────────────────────────────

    private void setupUsuario(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
    }

    private void setupAdmin() {
        admin = ActorFactory.administradorBancario(ADMIN_DOC, "Admin");
    }

    private void solicitarTarjeta(String document, String tipo) {
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo(tipo)
                        .conTitular("Juan Pablo")
                        .paraDocumento(document)
        );
    }

    // ── Scenario Outline steps ────────────────────────────────────────────────

    @Given("the user {string} requests a {string} card")
    public void theUserRequestsACard(String document, String cardType) {
        setupUsuario(document);
        solicitarTarjeta(document, cardType);
    }

    @When("the admin {string} the card with limit {double}")
    public void theAdminTheCardWithLimit(String action, double limit) {
        setupAdmin();
        Integer tarjetaId = usuario.recall("tarjetaId");
        if ("approves".equalsIgnoreCase(action)) {
            admin.attemptsTo(AprobarTarjeta.conId(tarjetaId).yLimiteCredito(limit));
        } else {
            admin.attemptsTo(RechazarTarjeta.conId(tarjetaId).porMotivo("Rechazada en prueba"));
        }
    }

    @Then("the card status should be {string}")
    public void theCardStatusShouldBe(String expectedStatus) {
        Actor actor = admin != null ? admin : usuario;
        assertThat(ElEstadoDeLaTarjeta.enLaRespuesta().answeredBy(actor))
                .as("Card status")
                .isEqualTo(expectedStatus);
    }

    // ── Named scenario steps ──────────────────────────────────────────────────

    @Given("a user with document {string} is authenticated")
    public void aUserWithDocumentIsAuthenticated(String document) {
        setupUsuario(document);
    }

    @When("the user requests a {string} card with holder name {string}")
    public void theUserRequestsACardWithHolderName(String cardType, String holderName) {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo(cardType)
                        .conTitular(holderName)
                        .paraDocumento(document)
        );
    }

    @When("the admin approves the card with a credit limit of {double}")
    public void theAdminApprovesTheCardWithACreditLimitOf(double limit) {
        setupAdmin();
        Integer tarjetaId = usuario.recall("tarjetaId");
        admin.attemptsTo(AprobarTarjeta.conId(tarjetaId).yLimiteCredito(limit));
    }

    @And("the card credit limit should be {double}")
    public void theCardCreditLimitShouldBe(double expectedLimit) {
        assertThat(ElCreditoDisponible.enLaRespuesta().answeredBy(admin))
                .as("Card credit limit")
                .isEqualTo(expectedLimit);
    }

    @When("the admin rejects the card with reason {string}")
    public void theAdminRejectsTheCardWithReason(String reason) {
        setupAdmin();
        Integer tarjetaId = usuario.recall("tarjetaId");
        admin.attemptsTo(RechazarTarjeta.conId(tarjetaId).porMotivo(reason));
    }

    @Given("the user {string} has an active {string} card approved by admin")
    public void theUserHasAnActiveCardApprovedByAdmin(String document, String cardType) {
        setupUsuario(document);
        solicitarTarjeta(document, cardType);
        setupAdmin();
        Integer tarjetaId = usuario.recall("tarjetaId");
        admin.attemptsTo(AprobarTarjeta.conId(tarjetaId));
    }

    @When("the user cancels the card")
    public void theUserCancelsTheCard() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        Integer tarjetaId = usuario.recall("tarjetaId");
        usuario.attemptsTo(
                net.serenitybdd.screenplay.rest.interactions.Delete
                        .from("/api/tarjetas/" + tarjetaId)
                        .with(req -> req.header("X-User-Documento", document))
        );
    }
}
