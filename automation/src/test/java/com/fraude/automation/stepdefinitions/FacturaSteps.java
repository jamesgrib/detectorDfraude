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

    private Actor usuario;
    private Integer facturaId;
    private Integer tarjetaId;

    // ── Shared setup ──────────────────────────────────────────────────────────

    private void setupUsuario(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
    }

    private void generarYGuardarFactura(String document) {
        usuario.attemptsTo(GenerarFacturasDePrueba.paraDocumento(document));
        facturaId = SerenityRest.lastResponse().jsonPath().getInt("facturas[0].id");
    }

    private Integer crearTarjetaActiva(String document, String tipo, Double limiteCredito) {
        Actor admin = ActorFactory.administradorBancario("ADMIN001", "Admin");
        usuario.attemptsTo(
                SolicitarTarjeta.deTipo(tipo)
                        .conTitular("Titular Test")
                        .paraDocumento(document)
        );
        Integer id = usuario.recall("tarjetaId");
        admin.attemptsTo(AprobarTarjeta.conId(id).yLimiteCredito(limiteCredito));
        return id;
    }

    // ── Scenario Outline steps ────────────────────────────────────────────────

    @Given("a user with document {string} has a pending invoice")
    public void aUserWithDocumentHasAPendingInvoice(String document) {
        setupUsuario(document);
        generarYGuardarFactura(document);
    }

    @And("the user has a {string} with sufficient funds")
    public void theUserHasAWithSufficientFunds(String paymentMethod) {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        if ("DEBITO".equalsIgnoreCase(paymentMethod)) {
            tarjetaId = crearTarjetaActiva(document, "DEBITO", null);
            // Recharge the debit card so it has funds
            usuario.attemptsTo(
                    net.serenitybdd.screenplay.rest.interactions.Post
                            .to("/api/tarjetas/" + tarjetaId + "/recargar")
                            .with(req -> req
                                    .header("X-User-Documento", document)
                                    .contentType("application/json")
                                    .body("{\"monto\": 500000, \"numeroCuenta\": \"ACC-000001\"}"))
            );
        } else {
            tarjetaId = crearTarjetaActiva(document, "CREDITO", 2000000.0);
        }
    }

    @When("the user pays the invoice using {string}")
    public void theUserPaysTheInvoiceUsing(String paymentMethod) {
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
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        usuario.attemptsTo(GenerarFacturasDePrueba.paraDocumento(document));
    }

    @And("the user has an active debit card with sufficient balance")
    public void theUserHasAnActiveDebitCardWithSufficientBalance() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        tarjetaId = crearTarjetaActiva(document, "DEBITO", null);
        usuario.attemptsTo(
                net.serenitybdd.screenplay.rest.interactions.Post
                        .to("/api/tarjetas/" + tarjetaId + "/recargar")
                        .with(req -> req
                                .header("X-User-Documento", document)
                                .contentType("application/json")
                                .body("{\"monto\": 500000, \"numeroCuenta\": \"ACC-000001\"}"))
        );
    }

    @When("the user pays the invoice with the debit card")
    public void theUserPaysTheInvoiceWithTheDebitCard() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        usuario.attemptsTo(
                PagarFactura.conId(facturaId)
                        .usandoTarjeta(tarjetaId)
                        .paraDocumento(document)
        );
    }

    @And("the user has an active credit card with available credit")
    public void theUserHasAnActiveCreditCardWithAvailableCredit() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        tarjetaId = crearTarjetaActiva(document, "CREDITO", 2000000.0);
    }

    @When("the user pays the invoice with the credit card")
    public void theUserPaysTheInvoiceWithTheCreditCard() {
        theUserPaysTheInvoiceWithTheDebitCard();
    }

    @And("the user has a debit card with zero balance")
    public void theUserHasADebitCardWithZeroBalance() {
        String document = usuario.recall(ActorFactory.NUM_DOCUMENTO);
        // Approve the card but do NOT recharge — saldo stays at 0.0
        tarjetaId = crearTarjetaActiva(document, "DEBITO", null);
    }

    @When("the user attempts to pay the invoice with the debit card")
    public void theUserAttemptsToPayTheInvoiceWithTheDebitCard() {
        theUserPaysTheInvoiceWithTheDebitCard();
    }
}
