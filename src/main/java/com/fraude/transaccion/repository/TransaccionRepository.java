package com.fraude.transaccion.repository;

import java.util.List;
import com.fraude.transaccion.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransaccionRepository extends JpaRepository<Transaccion, Integer> {

    List<Transaccion> findByCuentaOrigenId(String cuentaOrigenId);

    List<Transaccion> findByCuentaDestinoId(String cuentaDestinoId);

    List<Transaccion> findByEstadoTransaccionNombre(String nombre);

    // Conteos por nombre de estado
    long countByEstadoTransaccionNombre(String nombre);

    // Suma de montos aprobados
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t WHERE t.estadoTransaccion.nombre = 'APROBADA'")
    Double sumMontoAprobadas();

    // Suma de montos por nombre de estado
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t WHERE t.estadoTransaccion.nombre = :nombre")
    Double sumMontoByEstadoNombre(@Param("nombre") String nombre);

    // Transacciones agrupadas por cuenta origen (top cuentas)
    @Query("SELECT t.cuentaOrigenId, COUNT(t), COALESCE(SUM(t.monto), 0) FROM Transaccion t WHERE t.estadoTransaccion.nombre = 'APROBADA' GROUP BY t.cuentaOrigenId ORDER BY COUNT(t) DESC")
    List<Object[]> topCuentasPorActividad();

    // Historial de una cuenta específica (enviadas + recibidas)
    @Query("SELECT t FROM Transaccion t WHERE t.cuentaOrigenId = :cuenta OR t.cuentaDestinoId = :cuenta ORDER BY t.fechaCreacion DESC")
    List<Transaccion> findByCuenta(@Param("cuenta") String cuenta);
}
