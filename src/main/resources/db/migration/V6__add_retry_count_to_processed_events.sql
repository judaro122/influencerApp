-- V6: Add retry_count column to processed_events for DLQ routing
ALTER TABLE processed_events ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
