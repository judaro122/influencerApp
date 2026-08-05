CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (idempotency_key)
);

CREATE INDEX idx_idempotency_keys_tenant_id ON idempotency_keys(tenant_id);
