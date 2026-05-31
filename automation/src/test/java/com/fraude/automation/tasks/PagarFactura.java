package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.model.FacturaPagoRequest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

/**
 * Business-level task: pay an invoice using a card or account balance.
 * Calls POST /api/facturas/{id}/pagar with X-User-Documento header.
 *
 * Usage (with card):
 *   actor.attemptsTo(PagarFactura.conId(10).usandoTarjeta(3).paraDocumento("12345678"));
 *
 * Usage (with account):
 *   actor.attemptsTo(PagarFactura.conId(10).usandoCuenta("ACC-001").paraDocumento("12345678"));
 */
public class PagarFactura implements Task {

    private final Integer facturaId;
    private Integer tarjetaId;
    private String numeroCuenta;
    private String numDocumento;

    private PagarFactura(Integer facturaId) {
        this.facturaId = facturaId;
    }

    public static PagarFactura conId(Integer facturaId) {
        return new PagarFactura(facturaId);
    }

    public PagarFactura usandoTarjeta(Integer tarjetaId) {
        this.tarjetaId = tarjetaId;
        return this;
    }

    public PagarFactura usandoCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
        return this;
    }

    public PagarFactura paraDocumento(String numDocumento) {
        this.numDocumento = numDocumento;
        return this;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        FacturaPagoRequest body = FacturaPagoRequest.builder()
                .tarjetaId(tarjetaId)
                .numeroCuenta(numeroCuenta)
                .build();

        actor.attemptsTo(
                EnviarPOST.a("/api/facturas/" + facturaId + "/pagar")
                        .conHeader("X-User-Documento", numDocumento)
                        .conCuerpo(body),
                CapturarRespuesta.actual()
        );
    }
}
