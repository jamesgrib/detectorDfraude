package com.fraude.transaccion.repository;

import com.fraude.transaccion.model.TipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoTransaccionRepository extends JpaRepository<TipoTransaccion, Integer> {
    Optional<TipoTransaccion> findByNombre(String nombre);
}
