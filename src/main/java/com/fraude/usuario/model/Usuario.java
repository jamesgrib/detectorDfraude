package com.fraude.usuario.model;

import jakarta.persistence.*;
import lombok.*;
import com.fraude.rol.model.Rol;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @EmbeddedId
    private UsuarioId id;

    @Column(name = "rol_id")
    private Integer rolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Rol rol;

    private String nombre;

    private String apellido;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "estado_id")
    private Integer estadoId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}