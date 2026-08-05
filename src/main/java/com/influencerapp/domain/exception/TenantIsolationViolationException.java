package com.influencerapp.domain.exception;
/**
 * Runtime exception thrown when a cross-tenant access attempt is detected.
 *
 * @author judaro122
 * @since 1.0.0
 */



public class TenantIsolationViolationException extends DomainException {
    public TenantIsolationViolationException(String message) {
        super(message);
    }
}
