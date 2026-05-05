package com.fraude.tarjeta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_estado_tarjeta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 200)
    private String descripcion;
}
