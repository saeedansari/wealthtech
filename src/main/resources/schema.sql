CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS clients (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    description  TEXT,
    social_links TEXT[],
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    content         TEXT NOT NULL,
    content_vector  vector(768),
    summary         TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Trigram indexes for fast ILIKE substring matching
CREATE INDEX IF NOT EXISTS idx_clients_email_trgm ON clients USING GIN (email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_clients_first_name_trgm ON clients USING GIN (first_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_clients_last_name_trgm ON clients USING GIN (last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_clients_description_trgm ON clients USING GIN (description gin_trgm_ops);

-- HNSW index for fast approximate nearest neighbor search
CREATE INDEX IF NOT EXISTS idx_documents_content_vector ON documents
    USING hnsw (content_vector vector_cosine_ops);
