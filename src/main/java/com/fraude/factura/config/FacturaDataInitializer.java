package com.fraude.factura.config;

import com.fraude.factura.model.EstadoFactura;
import com.fraude.factura.model.Servicio;
import com.fraude.factura.repository.EstadoFacturaRepository;
import com.fraude.factura.repository.ServicioRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Inicializa los datos de referencia para servicios y estados de factura,
 * y migra filas existentes que aún usan las columnas legacy
 * tipo_servicio/estado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FacturaDataInitializer implements ApplicationRunner {

    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_DESCRIPCION = "descripcion";

    private final ServicioRepository servicioRepository;
    private final EstadoFacturaRepository estadoFacturaRepository;
    private final EntityManager entityManager;

    private static final List<Map<String, String>> SERVICIOS = List.of(
            Map.of(KEY_NOMBRE, "LUZ",        KEY_DESCRIPCION, "Servicio de energía eléctrica"),
            Map.of(KEY_NOMBRE, "AGUA",       KEY_DESCRIPCION, "Servicio de acueducto y alcantarillado"),
            Map.of(KEY_NOMBRE, "GAS",        KEY_DESCRIPCION, "Gas natural domiciliario"),
            Map.of(KEY_NOMBRE, "INTERNET",   KEY_DESCRIPCION, "Servicio de internet"),
            Map.of(KEY_NOMBRE, "TELEFONO",   KEY_DESCRIPCION, "Servicio de telefonía móvil"),
            Map.of(KEY_NOMBRE, "TELEVISION", KEY_DESCRIPCION, "Servicio de televisión por cable/streaming"),
            Map.of(KEY_NOMBRE, "SEGUROS",    KEY_DESCRIPCION, "Pago de seguros"));

    private static final List<Map<String, String>> ESTADOS = List.of(
            Map.of(KEY_NOMBRE, "PENDIENTE", KEY_DESCRIPCION, "Factura pendiente de pago"),
            Map.of(KEY_NOMBRE, "PAGADA",    KEY_DESCRIPCION, "Factura pagada"),
            Map.of(KEY_NOMBRE, "VENCIDA",   KEY_DESCRIPCION, "Factura vencida sin pagar"));

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedServicios();
        seedEstados();
        migrarFacturasLegacy();
    }

    private void seedServicios() {
        for (Map<String, String> s : SERVICIOS) {
            String nombre = s.get(KEY_NOMBRE);
            if (servicioRepository.findByNombre(nombre).isEmpty()) {
                servicioRepository.save(Servicio.builder()
                        .nombre(nombre)
                        .descripcion(s.get(KEY_DESCRIPCION))
                        .build());
                log.info("Servicio creado: {}", nombre);
            }
        }
    }

    private void seedEstados() {
        for (Map<String, String> e : ESTADOS) {
            String nombre = e.get(KEY_NOMBRE);
            if (estadoFacturaRepository.findByNombre(nombre).isEmpty()) {
                estadoFacturaRepository.save(EstadoFactura.builder()
                        .nombre(nombre)
                        .descripcion(e.get(KEY_DESCRIPCION))
                        .build());
                log.info("EstadoFactura creado: {}", nombre);
            }
        }
    }

    /**
     * Migra filas de tbl_factura que aún tienen tipo_servicio/estado como
     * columnas legacy (de antes de la normalización) y no tienen servicio_id
     * o estado_factura_id asignados.
     */
    private void migrarFacturasLegacy() {
        if (columnExists("tbl_factura", "tipo_servicio")) {
            int migratedServicio = entityManager.createNativeQuery("""
                    UPDATE tbl_factura
                    SET servicio_id = (
                        SELECT id FROM tbl_servicio WHERE nombre = tbl_factura.tipo_servicio
                    )
                    WHERE servicio_id IS NULL
                      AND tipo_servicio IS NOT NULL
                    """).executeUpdate();
            if (migratedServicio > 0) {
                log.info("Migración legacy: {} facturas con servicio asignado", migratedServicio);
            }
        }

        if (columnExists("tbl_factura", "estado")) {
            int migratedEstado = entityManager.createNativeQuery("""
                    UPDATE tbl_factura
                    SET estado_factura_id = (
                        SELECT id FROM tbl_estado_factura WHERE nombre = tbl_factura.estado
                    )
                    WHERE estado_factura_id IS NULL
                      AND estado IS NOT NULL
                    """).executeUpdate();
            if (migratedEstado > 0) {
                log.info("Migración legacy: {} facturas con estado asignado", migratedEstado);
            }
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
