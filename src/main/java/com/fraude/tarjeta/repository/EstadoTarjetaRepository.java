package com.fraude.tarjeta.repository;

import com.fraude.tarjeta.model.EstadoTarjeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoTarjetaRepository extends JpaRepository<EstadoTarjeta, Integer> {
    Optional<EstadoTarjeta> findByNombre(String nombre);
}
