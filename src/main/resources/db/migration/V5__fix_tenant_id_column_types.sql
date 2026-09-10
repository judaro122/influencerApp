-- V5: Fix tenant_id column type mismatch
-- Migrations V2, V3, V4 originally defined tenant_id as UUID, but the JPA entities
-- and the rest of the schema (V1) use VARCHAR(255). This migration converts any
-- remaining UUID columns to VARCHAR(255) for consistency.
-- The DO blocks make this idempotent: if the column is already VARCHAR(255)
-- (e.g., on a fresh database where V2-V4 were already corrected), the ALTER is skipped.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'idempotency_keys'
          AND column_name = 'tenant_id'
          AND data_type = 'uuid'
    ) THEN
        ALTER TABLE idempotency_keys ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'processed_events'
          AND column_name = 'tenant_id'
          AND data_type = 'uuid'
    ) THEN
        ALTER TABLE processed_events ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'outbox_events'
          AND column_name = 'tenant_id'
          AND data_type = 'uuid'
    ) THEN
        ALTER TABLE outbox_events ALTER COLUMN tenant_id TYPE VARCHAR(255) USING tenant_id::text;
    END IF;
END $$;
