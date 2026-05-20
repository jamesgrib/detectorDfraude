package com.fraude.transaccion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Positive
    @DecimalMax("999999999.99")
    @Column(name = "monto")
    private Double monto;

    @NotBlank
    @Size(max = 20)
    @Column(name = "cuenta_origen_id")
    private String cuentaOrigenId;

    @NotBlank
    @Size(max = 20)
    @Column(name = "cuenta_destino_id")
    private String cuentaDestinoId;

    /** FK normalizada a tbl_estado_transaccion */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id")
    private EstadoTransaccion estadoTransaccion;

    /** Compatibilidad hacia atrás: expone el ID del estado */
    @JsonProperty("estadoId")
    public Integer getEstadoId() {
        return estadoTransaccion != null ? estadoTransaccion.getId() : null;
    }

    /** Nombre del estado (PENDIENTE, APROBADA, RECHAZADA) */
    @JsonProperty("estadoNombre")
    public String getEstadoNombre() {
        return estadoTransaccion != null ? estadoTransaccion.getNombre() : null;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_transaccion_id")
    private TipoTransaccion tipoTransaccion;

    @Column(name = "fecha")
    private LocalDateTime fechaCreacion;

    /** Retrocompatibilidad: expone el nombre del tipo como string en el JSON */
    @JsonProperty("tipoTransaccionNombre")
    public String getTipoTransaccionNombre() {
        return tipoTransaccion != null ? tipoTransaccion.getNombre() : null;
    }
}