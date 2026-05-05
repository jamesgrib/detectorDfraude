package com.fraude.cuenta.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tbl_cuenta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta {

    @Id
    @Column(name = "numero_cuenta")
    private String numeroCuenta;

    @Column(name = "saldo")
    private BigDecimal saldo;

    @Column(name = "num_documento")
    private String numDocumento;
}