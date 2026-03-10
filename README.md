# WealthTech

A WealthTech automation platform that provides client management, document storage with vector embeddings, and a unified search API combining keyword search on clients with semantic search on documents.

## Prerequisites

- Java 17
- Docker and Docker Compose

## Setup

### Using Docker Compose

This starts PostgreSQL (with pgvector), Ollama (with embedding and LLM models), and the application:

```bash
docker compose up -d
```

The `ollama-init` service automatically pulls the required models (`nomic-embed-text` for embeddings, `tinyllama` for summarization). The application starts once PostgreSQL is healthy and models are pulled.

The API is available locally at `http://localhost:8080`.

### Running tests

Tests use Testcontainers to spin up PostgreSQL and Ollama automatically:

```bash
./gradlew test
```

### Swagger UI

Once the application is running, the API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

| Method | Endpoint                         | Description                         |
|--------|----------------------------------|-------------------------------------|
| POST   | `/v1/clients`                    | Create a new client                 |
| POST   | `/v1/clients/{id}/documents`     | Create a document for a client      |
| GET    | `/v1/search?q={query}`           | Search clients and documents        |

## Example Queries and Responses

### 1. Create a client

```bash
curl -s -X POST http://localhost:8080/v1/clients \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: YOUR_API_KEY_HERE" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@neviswealth.com",
    "description": "Senior financial advisor specializing in wealth management",
    "socialLinks": ["https://linkedin.com/in/johndoe"]
  }'
```

Response (`201 Created`):

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@neviswealth.com",
  "description": "Senior financial advisor specializing in wealth management",
  "socialLinks": ["https://linkedin.com/in/johndoe"],
  "createdAt": "2025-03-15T10:30:00",
  "updatedAt": "2025-03-15T10:30:00"
}
```

### 2. Create a document for a client

```bash
curl -s -X POST http://localhost:8080/v1/clients/a1b2c3d4-e5f6-7890-abcd-ef1234567890/documents \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: YOUR_API_KEY_HERE" \
  -d '{
    "title": "Utility Bill - March 2025",
    "content": "This is a utility bill from the electric company showing the residential address at 42 Wallaby Way, Sydney. The bill amount is $150.00."
  }'
```

Response (`201 Created`):

```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "clientId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "title": "Utility Bill - March 2025",
  "content": "This is a utility bill from the electric company showing the residential address at 42 Wallaby Way, Sydney. The bill amount is $150.00.",
  "createdAt": "2025-03-15T10:35:00"
}
```

### 3. Search by client name

Keyword search matches against client first name, last name, email, and description using `ILIKE`:

```bash
curl -s "http://localhost:8080/v1/search?q=John" \
     -H "X-API-KEY: YOUR_API_KEY_HERE"
```

Response (`200 OK`):

```json
{
  "clients": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "Senior financial advisor specializing in wealth management",
      "socialLinks": ["https://linkedin.com/in/johndoe"],
      "createdAt": "2025-03-15T10:30:00",
      "updatedAt": "2025-03-15T10:30:00"
    }
  ],
  "documents": []
}
```

### 4. Search by email domain

```bash
curl -s "http://localhost:8080/v1/search?q=neviswealth" \
     -H "X-API-KEY: YOUR_API_KEY_HERE"
```

Response (`200 OK`):

```json
{
  "clients": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "Senior financial advisor specializing in wealth management",
      "socialLinks": ["https://linkedin.com/in/johndoe"],
      "createdAt": "2025-03-15T10:30:00",
      "updatedAt": "2025-03-15T10:30:00"
    }
  ],
  "documents": []
}
```

### 5. Semantic document search

Documents are embedded using `nomic-embed-text` via Ollama. The search query is also embedded and compared using cosine distance in pgvector. 5 top documents with higher similarity score are returned. There is soft filter which checks that highest score is above a minimum accepted score:

```bash
curl -s "http://localhost:8080/v1/search?q=financial+statement" \
     -H "X-API-KEY: YOUR_API_KEY_HERE"
```

Response (`200 OK`):

```json
{
  "clients": [],
  "documents": [
    {
      "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "clientId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "Bank Statement Q1 2025",
      "content": "Quarterly bank statement showing financial transactions, account balance of $250,000 and investment portfolio details for the first quarter of 2025.",
      "summary": "A quarterly bank statement for Q1 2025 showing a balance of $250,000 with investment portfolio details.",
      "createdAt": "2025-03-15T10:36:00",
      "distance": 0.15
    }
  ]
}
```

### 6. Combined client and document results

A query can match both clients (by keyword) and documents (by semantic similarity):

```bash
curl -s "http://localhost:8080/v1/search?q=financial" \
     -H "X-API-KEY: YOUR_API_KEY_HERE"
```

Response (`200 OK`):

```json
{
  "clients": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "Senior financial advisor specializing in wealth management",
      "createdAt": "2025-03-15T10:30:00",
      "updatedAt": "2025-03-15T10:30:00"
    }
  ],
  "documents": [
    {
      "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "clientId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "Bank Statement Q1 2025",
      "content": "Quarterly bank statement showing financial transactions, account balance of $250,000 and investment portfolio details for the first quarter of 2025.",
      "summary": "A quarterly bank statement for Q1 2025 with a $250,000 balance and investment details.",
      "createdAt": "2025-03-15T10:36:00",
      "distance": 0.18
    }
  ]
}
```

### 7. No results

```bash
curl -s "http://localhost:8080/v1/search?q=zzzznonexistent" \
     -H "X-API-KEY: YOUR_API_KEY_HERE"
```

Response (`200 OK`):

```json
{
  "clients": [],
  "documents": []
}
```

### 8. Error responses (RFC-9457)

All errors follow the [RFC-9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format.

**Missing query parameter:**

```bash
curl -s "http://localhost:8080/v1/search" \
     -H "X-API-KEY: YOUR_API_KEY_HERE"

```

Response (`400 Bad Request`, content type `application/problem+json`):

```json
{
  "type": "/errors/types/validation",
  "title": "Missing Parameter",
  "status": 400,
  "detail": "Required request parameter 'q' for method parameter type String is not present",
  "instance": "/v1/search"
}
```

**Validation error:**

```bash
curl -s -X POST http://localhost:8080/v1/clients \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: YOUR_API_KEY_HERE" \
  -d '{"lastName": "Doe"}'
```

Response (`400 Bad Request`):

```json
{
  "type": "/errors/types/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/v1/clients",
  "errors": [
    "firstName: firstName is required",
    "email: email is required"
  ]
}
```

**Resource not found:**

```bash
curl -s -X POST http://localhost:8080/v1/clients/00000000-0000-0000-0000-000000000000/documents \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: YOUR_API_KEY_HERE" \
  -d '{"title": "Doc", "content": "Content"}'
```

Response (`404 Not Found`):

```json
{
  "type": "/errors/types/validation",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Client not found with id: 00000000-0000-0000-0000-000000000000",
  "instance": "/v1/clients/00000000-0000-0000-0000-000000000000/documents"
}
```

# Live Demo
The Search API is deployed and available at: https://wealthtech-production-7af1.up.railway.app

*Note:* Access requires an API Key. Please contact the author to request credentials.

API documentation is available at: https://wealthtech-production-7af1.up.railway.app/swagger-ui/index.html
