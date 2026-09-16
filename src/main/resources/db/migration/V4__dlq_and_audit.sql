-- V4: Dead Letter Queue (DLQ) and Append-Only Audit Logging

CREATE TABLE IF NOT EXISTS dead_letter_events (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    delivery_id VARCHAR(64) REFERENCES webhook_deliveries(id) ON DELETE SET NULL,
    organization_id VARCHAR(64) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    topic VARCHAR(128) NOT NULL,
    partition_num INT,
    offset_num BIGINT,
    failure_reason TEXT NOT NULL,
    exception_type VARCHAR(255),
    attempt_count INT NOT NULL DEFAULT 0,
    first_failed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_failed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNRESOLVED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    organization_id VARCHAR(64) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    actor_id VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(128),
    ip_address VARCHAR(64),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dlq_org ON dead_letter_events(organization_id);
CREATE INDEX idx_dlq_status ON dead_letter_events(status);
CREATE INDEX idx_audit_org ON audit_logs(organization_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
