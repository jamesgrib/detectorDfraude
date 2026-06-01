package com.fraude.automation.tasks;

import com.fraude.automation.interactions.CapturarRespuesta;
import com.fraude.automation.interactions.EnviarPOST;
import com.fraude.automation.model.FacturaPagoRequest;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

public class PagarFactura implements Performable {

    private final Integer facturaId;
    private final Integer tarjetaId;
    private final String numeroCuenta;
    private final String numDocumento;

    private PagarFactura(Integer facturaId, Integer tarjetaId, String numeroCuenta, String numDocumento) {
        this.facturaId = facturaId;
        this.tarjetaId = tarjetaId;
        this.numeroCuenta = numeroCuenta;
        this.numDocumento = numDocumento;
    }

    public static PagarFactura conId(Integer facturaId) {
        return new PagarFactura(facturaId, null, null, null);
    }

    public PagarFactura usandoTarjeta(Integer tarjetaId) {
        return new PagarFactura(this.facturaId, tarjetaId, this.numeroCuenta, this.numDocumento);
    }

    public PagarFactura usandoCuenta(String numeroCuenta) {
        return new PagarFactura(this.facturaId, this.tarjetaId, numeroCuenta, this.numDocumento);
    }

    public PagarFactura paraDocumento(String numDocumento) {
        return new PagarFactura(this.facturaId, this.tarjetaId, this.numeroCuenta, numDocumento);
    }

    @Override
    @Step("{0} pays invoice #facturaId")
    public <T extends Actor> void performAs(T actor) {
        FacturaPagoRequest body = FacturaPagoRequest.builder()
                .tarjetaId(tarjetaId)
                .numeroCuenta(numeroCuenta)
                .build();

        EnviarPOST.a("/api/facturas/" + facturaId + "/pagar")
                .conHeader("X-User-Documento", numDocumento)
                .conCuerpo(body)
                .performAs(actor);
        CapturarRespuesta.actual().performAs(actor);
    }
}
