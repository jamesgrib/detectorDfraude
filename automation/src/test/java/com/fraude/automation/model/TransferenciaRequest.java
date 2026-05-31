package com.fraude.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that maps to POST /api/transacciones request body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferenciaRequest {

    @JsonProperty("monto")
    private Double monto;

    @JsonProperty("cuentaOrigenId")
    private String cuentaOrigenId;

    @JsonProperty("cuentaDestinoId")
    private String cuentaDestinoId;
}
