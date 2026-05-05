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

/**
 * Inicializa los datos de referencia para marcas de tarjeta y estados de
 * tarjeta,
 * y migra filas existentes que aún usan columnas legacy.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TarjetaDataInitializer implements ApplicationRunner {

    private final MarcaTarjetaRepository marcaTarjetaRepository;
    private final EstadoTarjetaRepository estadoTarjetaRepository;
    private final EntityManager entityManager;

    private static final List<Map<String, String>> MARCAS = List.of(
            Map.of("nombre", "VISA", "descripcion", "Tarjeta Visa"),
            Map.of("nombre", "MASTERCARD", "descripcion", "Tarjeta Mastercard"),
            Map.of("nombre", "AMEX", "descripcion", "American Express"),
            Map.of("nombre", "UNKNOWN", "descripcion", "Marca no identificada"));

    private static final List<Map<String, String>> ESTADOS_TARJETA = List.of(
            Map.of("nombre", "ACTIVA", "descripcion", "Tarjeta activa y operativa"),
            Map.of("nombre", "PENDIENTE", "descripcion", "Esperando aprobación del administrador"),
            Map.of("nombre", "ELIMINADA", "descripcion", "Tarjeta eliminada por el usuario"),
            Map.of("nombre", "RECHAZADA", "descripcion", "Solicitud rechazada por el administrador"),
            Map.of("nombre", "BLOQUEADA", "descripcion", "Tarjeta bloqueada temporalmente"));

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
            if (marcaTarjetaRepository.findByNombre(m.get("nombre")).isEmpty()) {
                marcaTarjetaRepository.save(MarcaTarjeta.builder()
                        .nombre(m.get("nombre"))
                        .descripcion(m.get("descripcion"))
                        .build());
                log.info("MarcaTarjeta creada: {}", m.get("nombre"));
            }
        }
    }

    private void seedEstadosTarjeta() {
        for (Map<String, String> e : ESTADOS_TARJETA) {
            if (estadoTarjetaRepository.findByNombre(e.get("nombre")).isEmpty()) {
                estadoTarjetaRepository.save(EstadoTarjeta.builder()
                        .nombre(e.get("nombre"))
                        .descripcion(e.get("descripcion"))
                        .build());
                log.info("EstadoTarjeta creado: {}", e.get("nombre"));
            }
        }
    }

    /**
     * Migra filas de tbl_tarjeta que aún tienen la columna legacy marca
     * y no tienen marca_id asignado.
     */
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

    /**
     * Migra el campo estado_id legacy (1=ACTIVA, 2=PENDIENTE, 3=ELIMINADA,
     * 4=RECHAZADA)
     * para que apunte a la FK en tbl_estado_tarjeta.
     * Solo se ejecuta cuando hay filas cuyo estado_id no es un FK válido.
     */
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
