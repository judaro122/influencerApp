CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE tenants (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE UNIQUE INDEX idx_users_tenant_email ON users(tenant_id, email);

CREATE TABLE channels (
    channel_id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id),
    youtube_channel_id VARCHAR(255) NOT NULL,
    youtube_channel_title VARCHAR(500) NOT NULL,
    access_token_enc BYTEA NOT NULL,
    refresh_token_enc BYTEA NOT NULL,
    token_expiry TIMESTAMPTZ NOT NULL,
    scope VARCHAR(255) NOT NULL DEFAULT 'https://www.googleapis.com/auth/youtube.upload',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_channels_tenant_id ON channels(tenant_id);
CREATE UNIQUE INDEX idx_channels_tenant_youtube ON channels(tenant_id, youtube_channel_id);

CREATE TABLE videos (
    video_id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id),
    channel_id VARCHAR(255) NOT NULL REFERENCES channels(channel_id),
    filename VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    checksum VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    title VARCHAR(500),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    youtube_video_id VARCHAR(255),
    error_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_tenant_id ON videos(tenant_id);
CREATE INDEX idx_videos_tenant_status ON videos(tenant_id, status);
CREATE INDEX idx_videos_channel_id ON videos(channel_id);
CREATE UNIQUE INDEX uq_videos_tenant_video_id ON videos(tenant_id, video_id);
