package com.fraude.tarjeta.repository;

import com.fraude.tarjeta.model.MarcaTarjeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarcaTarjetaRepository extends JpaRepository<MarcaTarjeta, Integer> {
    Optional<MarcaTarjeta> findByNombre(String nombre);
}
