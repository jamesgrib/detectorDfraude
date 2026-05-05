package com.fraude.tarjeta.repository;

import com.fraude.tarjeta.model.Tarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TarjetaRepository extends JpaRepository<Tarjeta, Integer> {
    List<Tarjeta> findByNumDocumentoAndEstadoTarjetaNombre(String numDocumento, String estadoNombre);

    List<Tarjeta> findByNumDocumento(String numDocumento);

    List<Tarjeta> findByEstadoTarjetaNombre(String estadoNombre);

    List<Tarjeta> findAllByOrderByFechaCreacionDesc();
}
