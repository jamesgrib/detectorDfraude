package com.fraude.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO that maps to POST /api/usuarios/login request body.
 */
@Data
@Builder
public class LoginRequest {

    @JsonProperty("numDocumento")
    private String numDocumento;

    @JsonProperty("password")
    private String password;
}
