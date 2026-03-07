# Embedding API Options

## Free / Open Embedding APIs

### 1. Ollama (Locally Hosted — Completely Free) ✦ Recommended

- Run models like `nomic-embed-text` or `mxbai-embed-large` locally.
- No API key needed, no usage limits, no cost.
- Works well inside Docker Compose — just add an `ollama` service.
- Tradeoff: requires resources on the host machine.

### 2. Hugging Face Inference API (Free Tier)

- Models like `sentence-transformers/all-MiniLM-L6-v2` have a free tier.
- Rate-limited (~1,000 requests/hour on the free plan).
- 384 dimensions, good quality for semantic search.
- Tradeoff: rate limits make it unsuitable for bulk ingestion; no uptime guarantees on the free tier.

### 3. Google Gemini Embedding (Free Tier)

- Model: `text-embedding-004`.
- 1,500 requests/minute on the free tier — quite generous.
- 768 dimensions, good quality.
- Requires a Google API key but no billing setup for the free tier.
- Tradeoff: dependency on Google; free tier terms could change.

---

## Paid Options (For Reference)

| Provider | Model | Cost | Dimensions |
|----------|-------|------|------------|
| OpenAI | `text-embedding-3-small` | $0.02 / 1M tokens | 1536 |
| Cohere | `embed-english-v3.0` | Free trial, then $0.10 / 1M tokens | 1024 |
| Voyage AI | `voyage-3-lite` | Free trial | 512 |

---

## Recommendation: Ollama with `nomic-embed-text`

### Why Ollama?

1. **Truly free** — no API key, no rate limits, no billing surprises.
2. **Fits Docker Compose perfectly** — add a single service to `docker-compose.yml` and it just works. Reviewers run `docker-compose up` and everything is self-contained.
3. **No external dependency** — the project works offline and doesn't break if a third-party API changes its free tier.
4. **`nomic-embed-text` is strong** — 768 dimensions, competitive with OpenAI's smaller models on retrieval benchmarks.
5. **Simple HTTP API** — Ollama exposes `POST /api/embeddings` which is trivial to call from Spring Boot.

### Docker Compose Addition

```yaml
ollama:
  image: ollama/ollama:latest
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
```

With a startup script or init container that runs `ollama pull nomic-embed-text`.

### Spring Boot Integration

The application calls Ollama's local HTTP API:

```
POST http://ollama:11434/api/embeddings
{
  "model": "nomic-embed-text",
  "prompt": "the text to embed"
}
```

Response:

```json
{
  "embedding": [0.123, -0.456, ...]
}
```

### Swappability

The codebase defines an `EmbeddingService` interface. The Ollama implementation is the default, but switching to OpenAI, Cohere, or any other provider is a one-class change — no search logic needs to be modified.

```java
public interface EmbeddingService {
    float[] embed(String text);
}
```
