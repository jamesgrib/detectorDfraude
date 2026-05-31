package com.fraude.automation.stepdefinitions;

import com.fraude.automation.actors.ActorFactory;
import com.fraude.automation.questions.ElEstadoDeLaTransaccion;
import com.fraude.automation.tasks.AprobarTransferencia;
import com.fraude.automation.tasks.RechazarTransferencia;
import com.fraude.automation.tasks.RealizarTransferencia;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for transferencias.feature.
 */
public class TransferenciaSteps {

    private Actor usuario;
    private Actor admin;

    @Given("the user {string} transfers {double} from {string} to {string}")
    public void theUserTransfers(String document, double amount, String origen, String destino) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        usuario.attemptsTo(
                RealizarTransferencia.de(origen).a(destino).porMonto(amount)
        );
    }

    @Then("the transaction status should be {string}")
    public void theTransactionStatusShouldBe(String expectedStatus) {
        Actor actor = admin != null ? admin : usuario;
        assertThat(ElEstadoDeLaTransaccion.enLaRespuesta().answeredBy(actor))
                .as("Transaction status")
                .isEqualTo(expectedStatus);
    }

    @Given("a suspicious transfer is created by {string} from {string} to {string}")
    public void aSuspiciousTransferIsCreatedBy(String document, String origen, String destino) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        // Amount > 5,000,000 triggers PENDIENTE state
        usuario.attemptsTo(
                RealizarTransferencia.de(origen).a(destino).porMonto(6000000.0)
        );
        Integer id = SerenityRest.lastResponse().jsonPath().getInt("id");
        usuario.remember("pendingTransferId", id);
    }

    @When("the admin {string} approves the transfer")
    public void theAdminApprovesTheTransfer(String adminDoc) {
        admin = ActorFactory.administradorBancario(adminDoc, "Admin " + adminDoc);
        Integer id = usuario.recall("pendingTransferId");
        admin.attemptsTo(AprobarTransferencia.conId(id).comoAdmin(adminDoc));
    }

    @When("the admin {string} rejects the transfer")
    public void theAdminRejectsTheTransfer(String adminDoc) {
        admin = ActorFactory.administradorBancario(adminDoc, "Admin " + adminDoc);
        Integer id = usuario.recall("pendingTransferId");
        admin.attemptsTo(RechazarTransferencia.conId(id).comoAdmin(adminDoc));
    }
}
