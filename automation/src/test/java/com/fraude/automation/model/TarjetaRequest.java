package com.fraude.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that maps to POST /api/tarjetas request body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaRequest {

    @JsonProperty("nombreTitular")
    private String nombreTitular;

    @JsonProperty("tipoTarjeta")
    private String tipoTarjeta;
}
