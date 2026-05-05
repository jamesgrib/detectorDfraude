package com.fraude.usuario.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private boolean success;
    private String mensaje;
    private String email;
    private String nombre;
    private BigDecimal saldo;
    private String numeroCuenta;
    private String rol;
}

