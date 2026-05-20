package com.fraude.usuario.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    @Size(max = 20)
    private String numDocumento;

    @NotBlank
    @Size(max = 100)
    private String password;
}

