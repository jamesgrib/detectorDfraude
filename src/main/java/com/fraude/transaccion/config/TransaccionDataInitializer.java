package com.fraude.transaccion.config;

import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.TipoTransaccion;
import com.fraude.transaccion.repository.EstadoTransaccionRepository;
import com.fraude.transaccion.repository.TipoTransaccionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TransaccionDataInitializer implements ApplicationRunner {

    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final EstadoTransaccionRepository estadoTransaccionRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sembrarTipos();
        sembrarEstadosTransaccion();
        migrarTiposLegacy();
        migrarEstadosTransaccion();
    }

    private void sembrarTipos() {
        List<Object[]> tipos = List.of(
                new Object[] { "TRANSFERENCIA", "Movimiento de fondos entre cuentas" },
                new Object[] { "DEPOSITO",      "Ingreso de dinero a una cuenta" },
                new Object[] { "RETIRO",        "Extraccion de dinero de una cuenta" },
                new Object[] { "PAGO",          "Pago de servicios o facturas" },
                new Object[] { "COMPRA",        "Compra de bienes o servicios" });

        for (Object[] tipo : tipos) {
            String nombre     = (String) tipo[0];
            String descripcion = (String) tipo[1];
            if (tipoTransaccionRepository.findByNombre(nombre).isEmpty()) {
                tipoTransaccionRepository.save(TipoTransaccion.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .build());
                log.info("Tipo de transaccion creado: {}", nombre);
            }
        }
    }

    private void sembrarEstadosTransaccion() {
        List<Object[]> estados = List.of(
                new Object[] { "PENDIENTE",   "Transaccion en revision por posible fraude" },
                new Object[] { "APROBADA",    "Transaccion aprobada y ejecutada" },
                new Object[] { "RECHAZADA",   "Transaccion rechazada por fraude o saldo insuficiente" },
                new Object[] { "EN_REVISION", "Transaccion bajo revision manual del administrador" });

        for (Object[] estado : estados) {
            String nombre      = (String) estado[0];
            String descripcion = (String) estado[1];
            if (estadoTransaccionRepository.findByNombre(nombre).isEmpty()) {
                estadoTransaccionRepository.save(EstadoTransaccion.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .build());
                log.info("EstadoTransaccion creado: {}", nombre);
            }
        }
    }

    private void migrarTiposLegacy() {
        entityManager.createNativeQuery("""
                UPDATE tbl_transaccion
                SET tipo_transaccion_id = (
                    SELECT id FROM tbl_tipo_transaccion WHERE nombre = 'TRANSFERENCIA'
                )
                WHERE tipo_transaccion_id IS NULL
                """).executeUpdate();
        log.info("Migracion de tipos de transaccion completada");
    }

    private void migrarEstadosTransaccion() {
        int migrated = entityManager.createNativeQuery("""
                UPDATE tbl_transaccion
                SET estado_id = CASE estado_id
                    WHEN 4 THEN (SELECT id FROM tbl_estado_transaccion WHERE nombre = 'PENDIENTE')
                    WHEN 5 THEN (SELECT id FROM tbl_estado_transaccion WHERE nombre = 'APROBADA')
                    WHEN 6 THEN (SELECT id FROM tbl_estado_transaccion WHERE nombre = 'RECHAZADA')
                    ELSE (SELECT id FROM tbl_estado_transaccion WHERE nombre = 'PENDIENTE')
                END
                WHERE estado_id NOT IN (SELECT id FROM tbl_estado_transaccion)
                  OR estado_id IS NULL
                """).executeUpdate();

        if (migrated > 0) {
            log.info("Migracion estado transaccion: {} filas actualizadas", migrated);
        }
    }
}
