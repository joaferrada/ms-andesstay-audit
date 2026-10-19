package com.andesstay.ms_andesstay_audit.exception;

/**
 * Evento que no se puede registrar (por ejemplo, actor distinto al usuario del token). Se responde 400.
 */
public class InvalidAuditEventException extends RuntimeException {

    public InvalidAuditEventException(String message) {
        super(message);
    }
}
