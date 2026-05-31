package com.fraude.automation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO that maps to POST /api/facturas/{id}/pagar request body.
 * Either tarjetaId or numeroCuenta must be provided (not both).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacturaPagoRequest {

    @JsonProperty("tarjetaId")
    private Integer tarjetaId;

    @JsonProperty("numeroCuenta")
    private String numeroCuenta;
}
