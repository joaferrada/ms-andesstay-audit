package com.andesstay.ms_andesstay_audit.exception;

import com.andesstay.ms_andesstay_audit.entity.AuditEventType;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce excepciones a respuestas JSON {timestamp, status, error, message}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidAuditEventException.class)
    public ResponseEntity<Map<String, Object>> eventoInvalido(InvalidAuditEventException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Parametro invalido: " + ex.getName()
                + ". tipo: " + Arrays.toString(AuditEventType.values())
                + "; desde/hasta: ISO-8601 (ej. 2026-10-01T00:00:00Z)");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> bodyInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "Body invalido. Tipos validos: " + Arrays.toString(AuditEventType.values()));
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<Map<String, Object>> baseDeDatosNoDisponible(Exception ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Base de datos de auditoria no disponible");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
