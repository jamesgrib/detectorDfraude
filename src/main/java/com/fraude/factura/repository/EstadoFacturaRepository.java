package com.fraude.factura.repository;

import com.fraude.factura.model.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoFacturaRepository extends JpaRepository<EstadoFactura, Integer> {
    Optional<EstadoFactura> findByNombre(String nombre);
}
