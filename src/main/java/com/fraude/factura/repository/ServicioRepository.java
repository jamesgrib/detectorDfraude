package com.fraude.factura.repository;

import com.fraude.factura.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {
    Optional<Servicio> findByNombre(String nombre);
}
