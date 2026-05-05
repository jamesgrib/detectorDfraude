package com.fraude.factura.repository;

import com.fraude.factura.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    List<Factura> findByNumDocumentoOrderByFechaVencimientoDesc(String numDocumento);

    /** Busca facturas por documento y nombre del estado normalizado */
    List<Factura> findByNumDocumentoAndEstadoFacturaNombre(String numDocumento, String estadoNombre);
}
