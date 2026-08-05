CREATE TABLE processed_events (
    event_id VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_processed_events_tenant_id ON processed_events(tenant_id);
CREATE INDEX idx_processed_events_topic ON processed_events(topic);
