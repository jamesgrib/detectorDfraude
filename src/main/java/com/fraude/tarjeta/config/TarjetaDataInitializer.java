package com.fraude.tarjeta.config;

import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.repository.EstadoTarjetaRepository;
import com.fraude.tarjeta.repository.MarcaTarjetaRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TarjetaDataInitializer implements ApplicationRunner {

    private static final String KEY_NOMBRE      = "nombre";
    private static final String KEY_DESCRIPCION = "descripcion";

    private final MarcaTarjetaRepository marcaTarjetaRepository;
    private final EstadoTarjetaRepository estadoTarjetaRepository;
    private final EntityManager entityManager;

    private static final List<Map<String, String>> MARCAS = List.of(
            Map.of(KEY_NOMBRE, "VISA",        KEY_DESCRIPCION, "Tarjeta Visa"),
            Map.of(KEY_NOMBRE, "MASTERCARD",  KEY_DESCRIPCION, "Tarjeta Mastercard"),
            Map.of(KEY_NOMBRE, "AMEX",        KEY_DESCRIPCION, "American Express"),
            Map.of(KEY_NOMBRE, "UNKNOWN",     KEY_DESCRIPCION, "Marca no identificada"));

    private static final List<Map<String, String>> ESTADOS_TARJETA = List.of(
            Map.of(KEY_NOMBRE, "ACTIVA",    KEY_DESCRIPCION, "Tarjeta activa y operativa"),
            Map.of(KEY_NOMBRE, "PENDIENTE", KEY_DESCRIPCION, "Esperando aprobación del administrador"),
            Map.of(KEY_NOMBRE, "ELIMINADA", KEY_DESCRIPCION, "Tarjeta eliminada por el usuario"),
            Map.of(KEY_NOMBRE, "RECHAZADA", KEY_DESCRIPCION, "Solicitud rechazada por el administrador"),
            Map.of(KEY_NOMBRE, "BLOQUEADA", KEY_DESCRIPCION, "Tarjeta bloqueada temporalmente"));

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedMarcas();
        seedEstadosTarjeta();
        migrarTarjetasLegacy();
        migrarEstadosTarjeta();
    }

    private void seedMarcas() {
        for (Map<String, String> m : MARCAS) {
            String nombre = m.get(KEY_NOMBRE);
            if (marcaTarjetaRepository.findByNombre(nombre).isEmpty()) {
                marcaTarjetaRepository.save(MarcaTarjeta.builder()
                        .nombre(nombre)
                        .descripcion(m.get(KEY_DESCRIPCION))
                        .build());
                log.info("MarcaTarjeta creada: {}", nombre);
            }
        }
    }

    private void seedEstadosTarjeta() {
        for (Map<String, String> e : ESTADOS_TARJETA) {
            String nombre = e.get(KEY_NOMBRE);
            if (estadoTarjetaRepository.findByNombre(nombre).isEmpty()) {
                estadoTarjetaRepository.save(EstadoTarjeta.builder()
                        .nombre(nombre)
                        .descripcion(e.get(KEY_DESCRIPCION))
                        .build());
                log.info("EstadoTarjeta creado: {}", nombre);
            }
        }
    }

    private void migrarTarjetasLegacy() {
        if (!columnExists("tbl_tarjeta", "marca")) {
            return;
        }
        int migrated = entityManager.createNativeQuery("""
                UPDATE tbl_tarjeta
                SET marca_id = (
                    SELECT id FROM tbl_marca_tarjeta WHERE nombre = tbl_tarjeta.marca
                )
                WHERE marca_id IS NULL
                  AND marca IS NOT NULL
                """).executeUpdate();

        if (migrated > 0) {
            log.info("Migración legacy completada: {} tarjetas con marca asignada", migrated);
        }
    }

    private void migrarEstadosTarjeta() {
        int migrated = entityManager.createNativeQuery("""
                UPDATE tbl_tarjeta
                SET estado_id = CASE estado_id
                    WHEN 1 THEN (SELECT id FROM tbl_estado_tarjeta WHERE nombre = 'ACTIVA')
                    WHEN 2 THEN (SELECT id FROM tbl_estado_tarjeta WHERE nombre = 'PENDIENTE')
                    WHEN 3 THEN (SELECT id FROM tbl_estado_tarjeta WHERE nombre = 'ELIMINADA')
                    WHEN 4 THEN (SELECT id FROM tbl_estado_tarjeta WHERE nombre = 'RECHAZADA')
                    ELSE estado_id
                END
                WHERE estado_id NOT IN (SELECT id FROM tbl_estado_tarjeta)
                  AND estado_id IS NOT NULL
                """).executeUpdate();

        if (migrated > 0) {
            log.info("Migración estado tarjeta: {} filas actualizadas", migrated);
        }
    }

    private boolean columnExists(String table, String column) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = :table AND column_name = :column
                """)
                .setParameter("table", table)
                .setParameter("column", column)
                .getSingleResult();
        return count.intValue() > 0;
    }
}
