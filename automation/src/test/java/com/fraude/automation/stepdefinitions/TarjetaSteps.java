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
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.rest.interactions.Delete;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for tarjetas.feature.
 */
public class TarjetaSteps {

    private Actor usuario;
    private Actor admin;

    // ── Scenario Outline steps ────────────────────────────────────────────────

    @Given("a user with document {string} has a pending {string} card request")
    public void aUserWithDocumentHasAPendingCardRequest(String document, String cardType) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo(cardType)
                        .conTitular("Titular Test")
                        .paraDocumento(document)
        );
    }

    @When("the admin {string} the card request with limit {double}")
    public void theAdminTheCardRequestWithLimit(String action, double limit) {
        admin = ActorFactory.administradorBancario("ADMIN001", "Admin");
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
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
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

    @Given("a pending credit card request exists for document {string}")
    public void aPendingCreditCardRequestExistsForDocument(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo("CREDITO")
                        .conTitular("Titular Test")
                        .paraDocumento(document)
        );
    }

    @When("the admin approves the card with a credit limit of {double}")
    public void theAdminApprovesTheCardWithACreditLimitOf(double limit) {
        admin = ActorFactory.administradorBancario("ADMIN001", "Admin");
        Integer tarjetaId = usuario.recall("tarjetaId");
        admin.attemptsTo(AprobarTarjeta.conId(tarjetaId).yLimiteCredito(limit));
    }

    @And("the card credit limit should be {double}")
    public void theCardCreditLimitShouldBe(double expectedLimit) {
        assertThat(ElCreditoDisponible.enLaRespuesta().answeredBy(admin))
                .as("Card credit limit")
                .isEqualTo(expectedLimit);
    }

    @Given("a pending card request exists for document {string}")
    public void aPendingCardRequestExistsForDocument(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo("DEBITO")
                        .conTitular("Titular Test")
                        .paraDocumento(document)
        );
    }

    @When("the admin rejects the card with reason {string}")
    public void theAdminRejectsTheCardWithReason(String reason) {
        admin = ActorFactory.administradorBancario("ADMIN001", "Admin");
        Integer tarjetaId = usuario.recall("tarjetaId");
        admin.attemptsTo(RechazarTarjeta.conId(tarjetaId).porMotivo(reason));
    }

    @Given("a user with document {string} has an active card")
    public void aUserWithDocumentHasAnActiveCard(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        // Request and approve a card to get an active one
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo("DEBITO")
                        .conTitular("Titular Test")
                        .paraDocumento(document)
        );
        Integer tarjetaId = usuario.recall("tarjetaId");
        admin = ActorFactory.administradorBancario("ADMIN001", "Admin");
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
