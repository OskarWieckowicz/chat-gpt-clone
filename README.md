# ChatGPT Clone — Backend (Spring Boot + Spring AI + Ollama)

Spring Boot service that powers the ChatGPT-like UI. It manages conversations/messages, streams assistant replies over SSE, can enrich answers with optional web browsing, and supports RAG over PGVector. Models are served locally via Ollama.

## Quick start

1. Requirements

- Java 21
- Docker (for PostgreSQL + PGVector) or a local Postgres 16+
- Ollama installed and running (`ollama serve`)

2. Start services

```bash
# DB with PGVector (from this directory)
docker compose up -d

# Ollama in another terminal
ollama serve

# Pull required models
ollama pull llama3.2            # default text model (tools, RAG)
ollama pull llava               # multimodal (images)
ollama pull nomic-embed-text    # embeddings (768 dims)
```

3. Configure (application.yaml)

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2
      embedding:
        options:
          model: nomic-embed-text
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        max-document-batch-size: 1000
  datasource:
    url: jdbc:postgresql://localhost:5432/chatgpt_clone
    username: chat
    password: chat
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

google:
  cse:
    api-key: ${GOOGLE_CSE_API_KEY:}
    cx: ${GOOGLE_CSE_CX:}
    timeout-ms: 8000
```

4. Run backend

```bash
./mvnw spring-boot:run
```

## Model routing

- Text chat: default model is `llama3.2`. Tools and RAG can be used here.
- Multimodal chat (images): model is overridden to `llava` just for that request.
- Embeddings: `nomic-embed-text` (768 dims). Ensure all document ingestion and queries use the same embedding model to avoid dimension mismatch errors.

## RAG (PGVector)

- Upload PDFs per conversation. We chunk and embed with `nomic-embed-text` and store in the `vector_store` table (auto-created when `initialize-schema: true`).
- Retrieval is automatically enabled if any PDFs exist for the conversation or can be toggled via settings. A `QuestionAnswerAdvisor` filters by `conversationId` and respects `topK`.

### Document API

- `POST /api/conversations/{conversationId}/documents` — upload a PDF (ingests into vector store and stores file under `uploads/{conversationId}`)
- `GET  /api/conversations/{conversationId}/documents` — list attached PDFs `{ documentId, filename }`

## Web browsing (direct orchestration)

- `GoogleSearchService` (Google Programmable Search) + `WebFetchService` (Jsoup) to fetch lightweight context.
- Enable via conversation settings. Provide `GOOGLE_CSE_API_KEY` and `GOOGLE_CSE_CX`.

## Chat API (selected)

- `POST /api/conversations` — create
- `GET /api/conversations` — list
- `GET /api/conversations/{id}` — get
- `PATCH /api/conversations/{id}` — update title/settings
- `DELETE /api/conversations/{id}` — delete
- `GET /api/conversations/{id}/messages` — list messages
- `POST /api/chat/{conversationId}/messages` — stream assistant reply (SSE)
- `POST /api/chat/{conversationId}/messages/multimodal` — stream reply (text + images)

### Conversation settings (stored as JSON string)

```json
{
  "temperature": 0.7,
  "systemPrompt": "You are a helpful assistant.",
  "webAccessEnabled": false,
  "searchTopK": 3,
  "ragEnabled": false,
  "ragTopK": 3
}
```

## Troubleshooting

- Different vector dimensions X and Y: Ensure your embedding model matches the stored vectors. If you change the embedding model (e.g., 384→768), either re-ingest documents or clear the `vector_store` table.
- SSE errors when tools are attached: Some models may not support tool fields. We enable tools only for the text path.
- Payload too large on upload: adjust `spring.servlet.multipart.*` limits.

## Development

- Build/tests: `./mvnw -q -DskipTests=false test`
- Run locally: `./mvnw spring-boot:run`

## License

MIT
