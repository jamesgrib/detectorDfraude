package com.fraude.factura.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_estado_factura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", unique = true, nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
