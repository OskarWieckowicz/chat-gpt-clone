# ChatGPT Clone — Backend (Spring Boot + Spring AI)

Spring Boot service that powers the ChatGPT-like UI. It manages conversations/messages, streams assistant replies (SSE), and can optionally enrich answers with web browsing (Google Programmable Search).

## Quick start

1. Requirements

- Java 21
- PostgreSQL running locally

2. Configure

- Edit `src/main/resources/application.yaml` or use env vars.
- Important keys:
  - `SPRING_AI_OPENAI_BASE_URL`, `SPRING_AI_OPENAI_API_KEY`
  - `GOOGLE_CSE_API_KEY`, `GOOGLE_CSE_CX` (optional, for web browsing)

3. Run

```bash
./mvnw spring-boot:run
```

## Configuration (example)

```yaml
spring:
  ai:
    openai:
      base-url: http://localhost:12434/engines # example local provider
      api-key: dummy
      chat:
        options:
          model: ai/llama3.2:latest
  datasource:
    url: jdbc:postgresql://localhost:5432/chatgpt_clone
    username: chat
    password: chat
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false

google:
  cse:
    api-key: ${GOOGLE_CSE_API_KEY:}
    cx: ${GOOGLE_CSE_CX:}
    timeout-ms: 8000
```

## API (selected)

- `POST /api/conversations` — create
- `GET /api/conversations` — list
- `GET /api/conversations/{id}` — get
- `PATCH /api/conversations/{id}` — update title/settings
- `DELETE /api/conversations/{id}` — delete
- `GET /api/conversations/{id}/messages` — list messages
- `POST /api/chat/{conversationId}/messages` — stream assistant reply (SSE)

### Conversation settings (JSON string)

```json
{
  "temperature": 0.7,
  "systemPrompt": "You are a helpful assistant.",
  "webAccessEnabled": true,
  "searchTopK": 3
}
```

## Web browsing (direct approach)

- `GoogleSearchService` → Google CSE results
- `WebFetchService` → fetch & clean page text (Jsoup)
- `ChatService` → asks the model to craft a concise search query, searches/fetches when enabled, injects compact context, and streams the final answer

Notes: provide `GOOGLE_CSE_API_KEY` and `GOOGLE_CSE_CX`; guardrails: topK 1–5, content caps, URL validation.

## Development

- Build/tests: `./mvnw -q -DskipTests=false test`
- Run locally: `./mvnw spring-boot:run`

## More docs

See `HELP.md` for Spring Boot starter links and additional notes.

## License

MIT
