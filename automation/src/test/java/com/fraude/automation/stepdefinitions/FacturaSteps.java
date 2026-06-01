package com.fraude.automation.stepdefinitions;

import com.fraude.automation.actors.ActorFactory;
import com.fraude.automation.questions.ElEstadoDeLaFactura;
import com.fraude.automation.tasks.AprobarTarjeta;
import com.fraude.automation.tasks.GenerarFacturasDePrueba;
import com.fraude.automation.tasks.PagarFactura;
import com.fraude.automation.tasks.SolicitarTarjeta;
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
 * Step definitions for facturas.feature.
 */
public class FacturaSteps {

    private static final String ADMIN_DOC    = "12345678";
    private static final String CUENTA_USER  = "ACC-624489";

    private Actor usuario;
    private Integer facturaId;
    private Integer tarjetaId;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setupUsuario(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
    }

    private void generarFactura(String document) {
        usuario.attemptsTo(GenerarFacturasDePrueba.paraDocumento(document));
        facturaId = SerenityRest.lastResponse().jsonPath().getInt("facturas[0].id");
    }

    private Integer crearTarjetaActiva(String document, String tipo, Double limite) {
        Actor admin = ActorFactory.administradorBancario(ADMIN_DOC, "Admin");
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo(tipo)
                        .conTitular("Juan Pablo")
                        .paraDocumento(document)
        );
        Integer id = usuario.recall("tarjetaId");
        admin.attemptsTo(AprobarTarjeta.conId(id).yLimiteCredito(limite));
        return id;
    }

    private void recargarTarjeta(String document, Integer id) {
        usuario.attemptsTo(
                net.serenitybdd.screenplay.rest.interactions.Post
                        .to("/api/tarjetas/" + id + "/recargar")
                        .with(req -> req
                                .header("X-User-Documento", document)
                                .contentType("application/json")
                                .body("{\"monto\": 500000, \"numeroCuenta\": \"" + CUENTA_USER + "\"}"))
        );
    }

    // ── Scenario Outline steps ────────────────────────────────────────────────

    @Given("the user {string} has a pending invoice and a {string} card with funds")
    public void theUserHasAPendingInvoiceAndACardWithFunds(String document, String cardType) {
        setupUsuario(document);
        generarFactura(document);
        if ("DEBITO".equalsIgnoreCase(cardType)) {
            tarjetaId = crearTarjetaActiva(document, "DEBITO", null);
            recargarTarjeta(document, tarjetaId);
        } else {
            tarjetaId = crearTarjetaActiva(document, "CREDITO", 2000000.0);
        }
    }

    @When("the user pays the invoice with the card")
    public void theUserPaysTheInvoiceWithTheCard() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        usuario.attemptsTo(
                PagarFactura.conId(facturaId)
                        .usandoTarjeta(tarjetaId)
                        .paraDocumento(document)
        );
    }

    @Then("the invoice status should be {string}")
    public void theInvoiceStatusShouldBe(String expectedStatus) {
        assertThat(ElEstadoDeLaFactura.enLaRespuesta().answeredBy(usuario))
                .as("Invoice status")
                .isEqualTo(expectedStatus);
    }

    // ── Named scenario steps ──────────────────────────────────────────────────

    @When("the user requests test invoice generation")
    public void theUserRequestsTestInvoiceGeneration() {
        // usuario may be null if set up via TarjetaSteps.aUserWithDocumentIsAuthenticated
        // In that case, use document "7654321" as default
        String document = (usuario != null)
                ? usuario.recall(ActorFactory.NUM_DOCUMENTO)
                : "7654321";
        if (usuario == null) {
            setupUsuario(document);
        }
        usuario.attemptsTo(GenerarFacturasDePrueba.paraDocumento(document));
    }

    @And("the user has an active debit card with sufficient balance")
    public void theUserHasAnActiveDebitCardWithSufficientBalance() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        tarjetaId = crearTarjetaActiva(document, "DEBITO", null);
        recargarTarjeta(document, tarjetaId);
    }

    @When("the user pays the invoice with the debit card")
    public void theUserPaysTheInvoiceWithTheDebitCard() {
        theUserPaysTheInvoiceWithTheCard();
    }

    @And("the user has an active credit card with available credit")
    public void theUserHasAnActiveCreditCardWithAvailableCredit() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        tarjetaId = crearTarjetaActiva(document, "CREDITO", 2000000.0);
    }

    @When("the user pays the invoice with the credit card")
    public void theUserPaysTheInvoiceWithTheCreditCard() {
        theUserPaysTheInvoiceWithTheCard();
    }

    @Given("the user {string} has a pending invoice and a {string} card with no balance")
    public void theUserHasAPendingInvoiceAndACardWithNoBalance(String document, String cardType) {
        setupUsuario(document);
        generarFactura(document);
        // Approve card but do NOT recharge — saldo stays at 0.0
        tarjetaId = crearTarjetaActiva(document, cardType, null);
    }

    @When("the user attempts to pay the invoice with the card")
    public void theUserAttemptsToPayTheInvoiceWithTheCard() {
        theUserPaysTheInvoiceWithTheCard();
    }
}
