package com.influencerapp.domain.exception;
/**
 * Runtime exception representing a domain-level validation or business rule violation.
 *
 * @author judaro122
 * @since 1.0.0
 */



public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
