package com.fraude.factura.controller;

import com.fraude.factura.model.EstadoFactura;
import com.fraude.factura.model.Servicio;
import com.fraude.factura.repository.EstadoFacturaRepository;
import com.fraude.factura.repository.ServicioRepository;
import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.repository.EstadoTarjetaRepository;
import com.fraude.tarjeta.repository.MarcaTarjetaRepository;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.TipoTransaccion;
import com.fraude.transaccion.repository.EstadoTransaccionRepository;
import com.fraude.transaccion.repository.TipoTransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final ServicioRepository servicioRepository;
    private final EstadoFacturaRepository estadoFacturaRepository;
    private final MarcaTarjetaRepository marcaTarjetaRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final EstadoTarjetaRepository estadoTarjetaRepository;
    private final EstadoTransaccionRepository estadoTransaccionRepository;

    /** Lista todos los tipos de servicio disponibles */
    @GetMapping("/servicios")
    public ResponseEntity<List<Servicio>> getServicios() {
        return ResponseEntity.ok(servicioRepository.findAll());
    }

    /** Lista todos los estados de factura disponibles */
    @GetMapping("/estados-factura")
    public ResponseEntity<List<EstadoFactura>> getEstadosFactura() {
        return ResponseEntity.ok(estadoFacturaRepository.findAll());
    }

    /** Lista todas las marcas de tarjeta disponibles */
    @GetMapping("/marcas-tarjeta")
    public ResponseEntity<List<MarcaTarjeta>> getMarcasTarjeta() {
        return ResponseEntity.ok(marcaTarjetaRepository.findAll());
    }

    /** Lista todos los tipos de transacción disponibles */
    @GetMapping("/tipos-transaccion")
    public ResponseEntity<List<TipoTransaccion>> getTiposTransaccion() {
        return ResponseEntity.ok(tipoTransaccionRepository.findAll());
    }

    /** Lista todos los estados de tarjeta disponibles */
    @GetMapping("/estados-tarjeta")
    public ResponseEntity<List<EstadoTarjeta>> getEstadosTarjeta() {
        return ResponseEntity.ok(estadoTarjetaRepository.findAll());
    }

    /** Lista todos los estados de transacción disponibles */
    @GetMapping("/estados-transaccion")
    public ResponseEntity<List<EstadoTransaccion>> getEstadosTransaccion() {
        return ResponseEntity.ok(estadoTransaccionRepository.findAll());
    }
}
