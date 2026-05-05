package com.fraude.tarjeta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_marca_tarjeta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcaTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** VISA, MASTERCARD, AMEX, UNKNOWN */
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;
}
