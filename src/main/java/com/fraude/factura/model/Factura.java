package com.fraude.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_factura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "num_documento", nullable = false)
    private String numDocumento;

    /** FK normalizada a tbl_servicio */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "referencia", nullable = false)
    private String referencia;

    @Column(name = "monto", nullable = false)
    private Double monto;

    /** FK normalizada a tbl_estado_factura */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_factura_id")
    private EstadoFactura estadoFactura;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "tarjeta_id")
    private Integer tarjetaId;

    /** ID del PaymentIntent en Stripe */
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    /** Compatibilidad hacia atrás: devuelve el nombre del servicio como string */
    @JsonProperty("tipoServicio")
    public String getTipoServicio() {
        return servicio != null ? servicio.getNombre() : null;
    }

    /** Compatibilidad hacia atrás: devuelve el nombre del estado como string */
    @JsonProperty("estado")
    public String getEstado() {
        return estadoFactura != null ? estadoFactura.getNombre() : null;
    }
}
