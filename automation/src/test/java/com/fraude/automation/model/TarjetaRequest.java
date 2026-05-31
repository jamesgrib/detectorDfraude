package com.fraude.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO that maps to POST /api/tarjetas request body.
 */
@Data
@Builder
public class TarjetaRequest {

    @JsonProperty("nombreTitular")
    private String nombreTitular;

    @JsonProperty("tipoTarjeta")
    private String tipoTarjeta;
}
