package com.fraude.usuario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String numDocumento;
    private String nombre;
    private String apellido;
    private String email;
    private String password;
}
