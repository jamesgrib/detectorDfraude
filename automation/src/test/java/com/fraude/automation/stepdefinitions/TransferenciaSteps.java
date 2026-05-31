package com.fraude.automation.stepdefinitions;

import com.fraude.automation.actors.ActorFactory;
import com.fraude.automation.questions.ElEstadoDeLaTransaccion;
import com.fraude.automation.tasks.AprobarTransferencia;
import com.fraude.automation.tasks.RechazarTransferencia;
import com.fraude.automation.tasks.RealizarTransferencia;
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
 * Step definitions for transferencias.feature.
 */
public class TransferenciaSteps {

    private static final String CUENTA_ORIGEN  = "cuentaOrigen";
    private static final String CUENTA_DESTINO = "cuentaDestino";

    private Actor usuario;
    private Actor admin;

    // ── Scenario Outline steps ────────────────────────────────────────────────

    @Given("the origin account has a balance of {double}")
    public void theOriginAccountHasABalanceOf(double balance) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario("12345678", "Usuario Test");
        // Balance is managed by the backend; we store it as context for the report
        usuario.remember("expectedBalance", balance);
        usuario.remember(CUENTA_ORIGEN, "ACC-000001");
    }

    @And("the destination account exists")
    public void theDestinationAccountExists() {
        usuario.remember(CUENTA_DESTINO, "ACC-000002");
    }

    @When("the user transfers {double} from origin to destination")
    public void theUserTransfersFromOriginToDestination(double amount) {
        String origen  = usuario.recall(CUENTA_ORIGEN);
        String destino = usuario.recall(CUENTA_DESTINO);
        usuario.attemptsTo(
                RealizarTransferencia.de(origen).a(destino).porMonto(amount)
        );
    }

    @Then("the transaction status should be {string}")
    public void theTransactionStatusShouldBe(String expectedStatus) {
        Actor actor = usuario != null ? usuario : OnStage.theActorInTheSpotlight();
        assertThat(ElEstadoDeLaTransaccion.enLaRespuesta().answeredBy(actor))
                .as("Transaction status")
                .isEqualTo(expectedStatus);
    }

    // ── Named scenario steps ──────────────────────────────────────────────────

    @Given("a user with document {string} has an account with sufficient balance")
    public void aUserWithDocumentHasAnAccountWithSufficientBalance(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        usuario.remember(CUENTA_ORIGEN, "ACC-000001");
    }

    @Given("a user with document {string} has an account with insufficient balance")
    public void aUserWithDocumentHasAnAccountWithInsufficientBalance(String document) {
        OnStage.setTheStage(new OnlineCast());
        usuario = ActorFactory.usuarioBancario(document, "Usuario " + document);
        usuario.remember(CUENTA_ORIGEN, "ACC-000001");
    }

    @Given("a destination account exists")
    public void aDestinationAccountExists() {
        usuario.remember(CUENTA_DESTINO, "ACC-000002");
    }

    @When("the user performs a transfer of {double}")
    public void theUserPerformsATransferOf(double amount) {
        String origen  = usuario.recall(CUENTA_ORIGEN);
        String destino = usuario.recall(CUENTA_DESTINO);
        usuario.attemptsTo(
                RealizarTransferencia.de(origen).a(destino).porMonto(amount)
        );
    }

    @When("the user attempts a transfer of {double}")
    public void theUserAttemptsATransferOf(double amount) {
        theUserPerformsATransferOf(amount);
    }

    // ── Admin steps ───────────────────────────────────────────────────────────

    @Given("a pending transfer exists with id stored as {string}")
    public void aPendingTransferExistsWithIdStoredAs(String noteKey) {
        OnStage.setTheStage(new OnlineCast());
        // Create a suspicious transfer (> 5M) so it lands in PENDIENTE
        usuario = ActorFactory.usuarioBancario("12345678", "Usuario Test");
        usuario.remember(CUENTA_ORIGEN, "ACC-000001");
        usuario.remember(CUENTA_DESTINO, "ACC-000002");
        usuario.attemptsTo(
                RealizarTransferencia.de("ACC-000001").a("ACC-000002").porMonto(6000000.0)
        );
        Integer id = SerenityRest.lastResponse().jsonPath().getInt("id");
        usuario.remember(noteKey, id);
    }

    @When("the admin with document {string} approves the transfer")
    public void theAdminWithDocumentApprovesTheTransfer(String adminDoc) {
        admin = ActorFactory.administradorBancario(adminDoc, "Admin " + adminDoc);
        Integer id = usuario.recall("pendingTransferId");
        admin.attemptsTo(AprobarTransferencia.conId(id).comoAdmin(adminDoc));
    }

    @When("the admin with document {string} rejects the transfer")
    public void theAdminWithDocumentRejectsTheTransfer(String adminDoc) {
        admin = ActorFactory.administradorBancario(adminDoc, "Admin " + adminDoc);
        Integer id = usuario.recall("pendingTransferId");
        admin.attemptsTo(RechazarTransferencia.conId(id).comoAdmin(adminDoc));
    }
}
