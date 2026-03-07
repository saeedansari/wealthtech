# WealthTech Search API — Technical Proposal

## 1. Problem Statement

Build a Search API for a WealthTech automation platform that allows financial advisors to:

1. **Find clients** by matching against their name, email, or description (keyword / substring search).
2. **Find documents** by semantic similarity — e.g., searching "address proof" should also return documents containing "utility bill".
3. **(Optional)** Return a quick AI-generated summary of document content.

The API must follow REST conventions, return proper HTTP codes, and be fully documented.

---

## 2. Existing Project Context

The repository already contains a Spring Boot 4.0.3 skeleton (Java 17, Gradle). The proposal builds on top of this foundation rather than starting from scratch.

---

## 3. High-Level Architecture

```
┌─────────────┐       ┌──────────────────────────┐
│   Client     │──────▶│  Spring Boot REST API     │
│  (Advisor)   │       │  (Controllers / Services) │
└─────────────┘       └──────────┬────────────────┘
                                 │
                    ┌────────────▼────────────────┐
                    │  PostgreSQL + pgvector       │
                    │  (Data store + Vector search │
                    │   + Keyword search)          │
                    └────────────┬────────────────┘
                                 │
                    ┌────────────▼────────────────┐
                    │  Ollama (local)              │
                    │  nomic-embed-text (embeddings│
                    │  + Summarization via LLM)    │
                    └─────────────────────────────┘
```

This is a deliberately simple two-service architecture (PostgreSQL + Ollama) that avoids unnecessary infrastructure while meeting all requirements.

---

## 4. Technology Choices & Rationale

### 4.1 Language & Framework: **Java 17 + Spring Boot 4.0.3** (already in place)

| Decision | Rationale |
|----------|-----------|
| Keep Java / Spring Boot | The skeleton is already set up. Spring Boot is the industry standard for production REST APIs in Java — mature ecosystem, excellent testing support, wide hiring pool. |
| Java 17 | Already configured. LTS release with good language features (records, sealed classes, text blocks). |

### 4.2 Database & Search: **PostgreSQL + pgvector**

| Decision | Rationale |
|----------|-----------|
| PostgreSQL over H2/SQLite | Production-grade RDBMS. Needed for relational integrity between clients and documents. |
| PostgreSQL over MongoDB | The data model is inherently relational (clients → documents). A relational DB enforces referential integrity and makes joins straightforward. |
| **pgvector over Elasticsearch** | Eliminates an entire infrastructure dependency. pgvector handles cosine-similarity kNN queries over embeddings directly in PostgreSQL. The client keyword search requirements (substring matching on name/email) are simple enough for PostgreSQL's `ILIKE` with `pg_trgm` indexing. This avoids the dual-write consistency problem entirely — data and vectors live in the same database, same transaction. |
| pgvector over standalone vector DBs (Pinecone, Weaviate, Qdrant) | Unnecessary infrastructure for this scale. pgvector with HNSW indexing handles millions of vectors. |
| Spring Data JPA + Hibernate | Reduces boilerplate for CRUD operations. Perfectly suited for the `POST /clients` and `POST /clients/{id}/documents` endpoints. |
| `pg_trgm` extension for client search | Enables trigram-based similarity and fast `ILIKE` queries with GIN indexes. Searching "neviswealth" will match `john.doe@neviswealth.com` via substring matching. |

### 4.3 Embeddings: **Ollama with `nomic-embed-text`** (locally hosted, free)

| Decision | Rationale |
|----------|-----------|
| Ollama over external API (OpenAI, Cohere) | Completely free — no API key, no rate limits, no billing. Self-contained within Docker Compose so reviewers can run `docker-compose up` with zero external dependencies. |
| `nomic-embed-text` | 768 dimensions, competitive with OpenAI's smaller models on retrieval benchmarks. Good balance of quality and resource usage. |
| Abstracted behind an interface | The embedding provider is behind an `EmbeddingService` interface so it can be swapped (e.g., to OpenAI or a different local model) without touching search logic. |

### 4.4 Summarization (Optional Feature): **Ollama with an LLM model**

| Decision | Rationale |
|----------|-----------|
| Ollama LLM (e.g., `llama3.2` or `mistral`) | Same Ollama service already running for embeddings. No additional infrastructure needed. A short prompt like "Summarize this document in 2-3 sentences" produces good summaries. |
| On-demand, not pre-computed | Summaries are generated at search time (or lazily cached) rather than at ingestion. This avoids storing stale summaries when documents are updated and keeps write latency low. |
| Cache summaries in DB | Once generated, store the summary alongside the document to avoid repeated generation for the same content. |

### 4.5 Containerization: **Docker Compose**

| Decision | Rationale |
|----------|-----------|
| Docker Compose | Explicitly required in the deliverables. Orchestrates the app, PostgreSQL (with pgvector), and Ollama in a single `docker-compose up`. |
| Multi-stage Dockerfile | Keeps the final image small (build with Gradle, run with JRE-only image). |

### 4.6 API Documentation: **SpringDoc OpenAPI (Swagger UI)**

| Decision | Rationale |
|----------|-----------|
| SpringDoc over Springfox | Springfox is abandoned. SpringDoc is the actively maintained OpenAPI 3.x library for Spring Boot. Produces a Swagger UI at `/swagger-ui.html` and a JSON spec at `/v3/api-docs`. |

### 4.7 Testing: **JUnit 5 + Testcontainers**

| Decision | Rationale |
|----------|-----------|
| JUnit 5 | Already configured in the skeleton. Industry standard. |
| Testcontainers | Spins up a real PostgreSQL (with pgvector) container during integration tests. Tests against real infrastructure, not mocks, which catches real issues. |
| MockMvc | For controller-level tests that verify HTTP status codes, request validation, and response shapes without starting the full app. |

---

## 5. Data Model

### 5.1 PostgreSQL Schema

```sql
-- Enable extensions
CREATE EXTENSION IF NOT EXISTS vector;    -- pgvector for semantic search
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- trigram indexing for ILIKE performance

CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    description TEXT,
    social_links TEXT[],       -- PostgreSQL array type
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- Trigram indexes for fast ILIKE substring matching
CREATE INDEX idx_clients_email_trgm ON clients USING GIN (email gin_trgm_ops);
CREATE INDEX idx_clients_first_name_trgm ON clients USING GIN (first_name gin_trgm_ops);
CREATE INDEX idx_clients_last_name_trgm ON clients USING GIN (last_name gin_trgm_ops);
CREATE INDEX idx_clients_description_trgm ON clients USING GIN (description gin_trgm_ops);

CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    content         TEXT NOT NULL,
    content_vector  vector(768),  -- pgvector column for nomic-embed-text embeddings
    summary         TEXT,         -- cached LLM-generated summary
    created_at      TIMESTAMP DEFAULT NOW()
);

-- HNSW index for fast approximate nearest neighbor search
CREATE INDEX idx_documents_content_vector ON documents
    USING hnsw (content_vector vector_cosine_ops);
```

There is no separate search index or secondary data store. PostgreSQL is the single source of truth for both relational data and vector embeddings.

---

## 6. Search Strategy

The `/search?q=<query>` endpoint performs a **combined search** across both clients and documents:

### 6.1 Client Search (Keyword / Substring)

Use PostgreSQL `ILIKE` with `pg_trgm` GIN indexes for fast substring matching:

```sql
SELECT *, similarity(email, :query) AS score
FROM clients
WHERE email ILIKE '%' || :query || '%'
   OR first_name ILIKE '%' || :query || '%'
   OR last_name ILIKE '%' || :query || '%'
   OR description ILIKE '%' || :query || '%'
ORDER BY score DESC
LIMIT 20;
```

This directly satisfies the requirement: searching "NevisWealth" matches `john.doe@neviswealth.com` via substring matching. The `pg_trgm` `similarity()` function provides a relevance score for ranking.

### 6.2 Document Search (Semantic via pgvector)

1. **Generate an embedding** for the query string by calling Ollama's `/api/embeddings` endpoint.
2. **Run a cosine similarity search** using pgvector:

```sql
SELECT d.*, 1 - (d.content_vector <=> :query_vector) AS score
FROM documents d
ORDER BY d.content_vector <=> :query_vector
LIMIT 20;
```

The `<=>` operator computes cosine distance. The HNSW index makes this an efficient approximate nearest neighbor search.

This ensures "address proof" returns documents containing "utility bill" because the semantic similarity comes from the embedding model — both phrases map to nearby points in the vector space.

### 6.3 Response Format

```json
{
  "clients": [
    {
      "id": "uuid",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "...",
      "social_links": ["..."],
      "score": 0.95
    }
  ],
  "documents": [
    {
      "id": "uuid",
      "client_id": "uuid",
      "title": "Utility Bill - March 2025",
      "content": "...",
      "summary": "A utility bill from March 2025 showing the client's residential address.",
      "score": 0.87
    }
  ]
}
```

Results are grouped by type (clients vs documents) and sorted by relevance score within each group. The `summary` field is populated on-demand for document results.

---

## 7. Project Structure

```
src/main/java/com/nevis/
├── WealthTechApplication.java
├── config/
│   └── OpenApiConfig.java
├── controller/
│   ├── ClientController.java        # POST /clients
│   ├── DocumentController.java      # POST /clients/{id}/documents
│   └── SearchController.java        # GET /search
├── dto/
│   ├── ClientRequest.java
│   ├── ClientResponse.java
│   ├── DocumentRequest.java
│   ├── DocumentResponse.java
│   └── SearchResponse.java
├── entity/
│   ├── Client.java                  # JPA entity
│   └── Document.java                # JPA entity
├── repository/
│   ├── ClientRepository.java        # Spring Data JPA + custom ILIKE queries
│   └── DocumentRepository.java      # Spring Data JPA + native pgvector queries
├── service/
│   ├── ClientService.java
│   ├── DocumentService.java
│   ├── EmbeddingService.java        # Interface for embedding providers
│   ├── OllamaEmbeddingService.java  # Ollama implementation
│   ├── SearchService.java           # Orchestrates client + document search
│   └── SummarizationService.java    # Ollama LLM summarization
└── exception/
    ├── GlobalExceptionHandler.java  # @ControllerAdvice
    └── ResourceNotFoundException.java
```

Note: no `search/` package or Elasticsearch config is needed. Search queries are native SQL via Spring Data JPA repositories.

---

## 8. Key Implementation Details

### 8.1 Write Path (Single-Write, No Sync Issues)

When a client is created:
1. Persist to PostgreSQL. Done.

When a document is created:
1. Generate embedding via Ollama's `/api/embeddings` endpoint.
2. Persist the document **and** its embedding vector to PostgreSQL in a single transaction.

There is no dual-write problem. Data and vectors are in the same database, committed atomically.

### 8.2 pgvector Integration with JPA

The `content_vector` column is mapped using a custom Hibernate type or a native query approach:

```java
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    private String title;
    private String content;

    @Column(name = "content_vector", columnDefinition = "vector(768)")
    private float[] contentVector;

    private String summary;
    private LocalDateTime createdAt;
}
```

Vector search uses native queries:

```java
@Query(value = "SELECT d.*, 1 - (d.content_vector <=> CAST(:vector AS vector)) AS score " +
               "FROM documents d " +
               "ORDER BY d.content_vector <=> CAST(:vector AS vector) " +
               "LIMIT :limit",
       nativeQuery = true)
List<Object[]> findBySemanticSimilarity(@Param("vector") String vector,
                                         @Param("limit") int limit);
```

### 8.3 Client Keyword Search with pg_trgm

```java
@Query(value = "SELECT c.*, GREATEST(" +
               "  similarity(c.email, :query), " +
               "  similarity(c.first_name, :query), " +
               "  similarity(c.last_name, :query), " +
               "  similarity(COALESCE(c.description, ''), :query)" +
               ") AS score " +
               "FROM clients c " +
               "WHERE c.email ILIKE '%' || :query || '%' " +
               "   OR c.first_name ILIKE '%' || :query || '%' " +
               "   OR c.last_name ILIKE '%' || :query || '%' " +
               "   OR c.description ILIKE '%' || :query || '%' " +
               "ORDER BY score DESC LIMIT :limit",
       nativeQuery = true)
List<Object[]> searchClients(@Param("query") String query,
                              @Param("limit") int limit);
```

### 8.4 Error Handling

- `400 Bad Request` — validation failures (missing required fields, invalid email format).
- `404 Not Found` — client ID not found when posting a document.
- `500 Internal Server Error` — unexpected failures (Ollama down, database issues).
- A `@ControllerAdvice` global exception handler maps exceptions to consistent JSON error responses.

### 8.5 Input Validation

- Use Jakarta Bean Validation (`@NotBlank`, `@Email`, etc.) on request DTOs.
- Spring Boot auto-validates annotated `@RequestBody` parameters.

---

## 9. Docker Compose Setup

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wealthtech
      SPRING_DATASOURCE_USERNAME: wealthtech
      SPRING_DATASOURCE_PASSWORD: wealthtech
      OLLAMA_BASE_URL: http://ollama:11434
    depends_on:
      postgres:
        condition: service_healthy
      ollama-init:
        condition: service_completed_successfully

  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: wealthtech
      POSTGRES_USER: wealthtech
      POSTGRES_PASSWORD: wealthtech
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wealthtech"]
      interval: 5s
      timeout: 3s
      retries: 5

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:11434/api/tags || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10

  ollama-init:
    image: ollama/ollama:latest
    entrypoint: ["/bin/sh", "-c", "ollama pull nomic-embed-text && ollama pull llama3.2"]
    environment:
      OLLAMA_HOST: ollama:11434
    depends_on:
      ollama:
        condition: service_healthy

volumes:
  postgres_data:
  ollama_data:
```

Key points:
- Uses `pgvector/pgvector:pg16` — an official PostgreSQL image with pgvector pre-installed.
- An `ollama-init` service pulls the required models before the app starts.
- No Elasticsearch service needed. Only 3 services (app, PostgreSQL, Ollama) + an init container.

---

## 10. Testing Strategy

| Layer | Tool | What It Covers |
|-------|------|----------------|
| Unit | JUnit 5 + Mockito | Service logic, DTO mapping, validation, score ranking |
| Integration | Testcontainers (pgvector) | Repository queries, vector search, ILIKE search, embedding round-trips |
| Controller | MockMvc | HTTP status codes, request/response serialization, validation error messages |
| End-to-end | Docker Compose + REST Assured | Full flow: create client → create document → search and verify results |

### Key Test Cases
- Search by client email substring → correct client returned.
- Search by client name → correct client returned.
- Semantic search: "address proof" → returns document with "utility bill" content.
- Empty query → 400 Bad Request.
- Document for nonexistent client → 404 Not Found.
- Missing required fields → 400 with validation errors.
- Vector search returns results ordered by cosine similarity.

---

## 11. Alternatives Considered

| Alternative | Why Not Chosen |
|-------------|----------------|
| **Elasticsearch** | Adds a significant infrastructure dependency (extra service, dual-write consistency issues, separate index management). The client keyword search is simple enough for PostgreSQL `ILIKE` + `pg_trgm`. Semantic search is handled by pgvector. Elasticsearch would be warranted if we needed advanced features like fuzzy matching, autocomplete, faceted search, or complex relevance tuning — the current requirements don't call for these. |
| **Python / FastAPI** | The project skeleton is already Java/Spring Boot. Switching would discard existing work. |
| **Standalone vector DB (Pinecone, Weaviate, Qdrant)** | Unnecessary infrastructure for this scale. pgvector with HNSW indexing is sufficient and avoids sync issues. |
| **External embedding API (OpenAI, Cohere)** | Adds cost, requires API keys, and creates an external dependency. Ollama is free, runs locally, and keeps `docker-compose up` fully self-contained. |
| **Pre-computed summaries at ingestion** | Adds write latency, can become stale. On-demand + caching is more flexible. |
| **Kafka/RabbitMQ for async processing** | Over-engineering for this scope. With a single database (no dual-write), there's no need for event-driven sync. |

---

## 12. Scalability Considerations

While the current scope is small, the architecture supports growth:

- **pgvector HNSW index** handles millions of vectors with sub-millisecond query times.
- **PostgreSQL** can be scaled vertically or with read replicas.
- **`pg_trgm` GIN indexes** keep `ILIKE` queries fast even with large client tables.
- **Ollama embedding calls** can be batched for bulk document ingestion.
- **Summary caching** in the database prevents repeated LLM calls for the same content.
- **The search response** is structured to support pagination (add `page` and `size` query params later).
- **The `EmbeddingService` interface** allows swapping Ollama for a hosted API (OpenAI, etc.) if higher throughput is needed without local compute.

---

## 13. Deliverables Checklist

- [x] Source code in a repo
- [x] `docker-compose.yml` for reproducibility
- [x] Tests for core logic and edge cases
- [x] `README.md` with setup instructions and example queries
- [x] API documentation via Swagger UI (`/swagger-ui.html`)
- [ ] (Bonus) Deployment with credentials

---

## 14. Dependencies to Add to `build.gradle`

```groovy
dependencies {
    // Web
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // JPA + PostgreSQL
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'

    // pgvector JPA support
    implementation 'com.pgvector:pgvector:0.1.6'

    // Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // OpenAPI / Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0'

    // HTTP client for Ollama API calls
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'io.rest-assured:rest-assured'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Note: no Elasticsearch dependencies needed. The `pgvector` Java library provides the Hibernate type mapping for the `vector` column type.

---

## 15. Summary

This proposal builds a **Java/Spring Boot REST API** backed by **PostgreSQL with pgvector** as the single data store for both relational data and vector embeddings. Client search uses **`ILIKE` with `pg_trgm` indexes** for fast substring matching. Document search uses **pgvector cosine similarity** over embeddings generated by **Ollama's `nomic-embed-text` model** (locally hosted, free, no API key required). An optional LLM-powered summarization service uses Ollama to provide concise document summaries on demand. The entire stack is self-contained — `docker-compose up` starts PostgreSQL, Ollama, and the application with zero external dependencies. Testing uses **Testcontainers** for production-realistic integration tests.
