package com.fraude.cuenta.repository;

import com.fraude.cuenta.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, String> {
    Optional<Cuenta> findByNumDocumento(String numDocumento);
}