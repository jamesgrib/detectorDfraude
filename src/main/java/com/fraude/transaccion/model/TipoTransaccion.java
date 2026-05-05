package com.fraude.transaccion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_tipo_transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre; // TRANSFERENCIA, DEPOSITO, RETIRO, PAGO, COMPRA

    @Column(name = "descripcion")
    private String descripcion;
}
