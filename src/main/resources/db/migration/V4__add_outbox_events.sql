CREATE TABLE outbox_events (
    id BIGSERIAL NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    schema_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    error_message TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_status ON outbox_events(status);
CREATE INDEX idx_outbox_events_tenant_id ON outbox_events(tenant_id);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id, event_type);
CREATE UNIQUE INDEX uq_outbox_events_aggregate_event ON outbox_events(aggregate_id, event_type);
