package com.fraude.transaccion.service;

import com.fraude.transaccion.model.Transaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FraudeService {

    public String evaluarFraude(Transaccion transaccion) {
        try {
            log.info("Evaluando fraude para transacción: monto={}", transaccion.getMonto());

            if (transaccion.getMonto() == null) {
                log.warn("⚠Monto es nulo");
                return "RECHAZADA";
            }

            double monto = transaccion.getMonto();

            if (monto > 5000000) {
                log.info("Monto sospechoso (> $5,000,000): $" + monto);
                return "PENDIENTE"; // Requiere revisión
            } else {
                log.info("Monto bajo (≤ $5,000,000): $" + monto);
                return "APROBADA";
            }
        } catch (Exception e) {
            log.error("Error al evaluar fraude", e);
            return "RECHAZADA";
        }
    }
}
