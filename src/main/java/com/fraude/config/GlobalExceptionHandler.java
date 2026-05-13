package com.fraude.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Manejador global de excepciones para todos los controllers.
 * Centraliza el manejo de errores y elimina try/catch repetitivos.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("Error de acceso: {}", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "error",   ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                "success", false
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validacion fallida: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error",   ex.getMessage(),
                "success", false
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.error("Error de estado: {}", ex.getMessage());
        return ResponseEntity.internalServerError().body(Map.of(
                "error",   ex.getMessage(),
                "success", false
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(Map.of(
                "error",   "Error interno del servidor",
                "success", false
        ));
    }
}
