package com.influencerapp.domain.exception;

public class TenantIsolationViolationException extends DomainException {
    public TenantIsolationViolationException(String message) {
        super(message);
    }
}
