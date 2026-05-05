package com.fraude.transaccion.repository;

import com.fraude.transaccion.model.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoTransaccionRepository extends JpaRepository<EstadoTransaccion, Integer> {
    Optional<EstadoTransaccion> findByNombre(String nombre);
}
