# Phase 3 — RAG (Retrieval Augmented Generation) Implementation Summary

## Overview

Phase 3 (Days 7–11 of the learning roadmap) adds RAG capabilities to the log analyzer: embeddings, an in-memory vector store, smart log chunking, and a retrieval-augmented generation pipeline. All changes are **purely additive** — no existing Phase 2 code was modified.

---

## New Dependency

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-all-minilm-l6-v2</artifactId>
    <version>0.31.0</version>
</dependency>
```

**AllMiniLmL6V2EmbeddingModel** — runs in-process via ONNX runtime. No external API key or service needed for generating embeddings.

---

## New Files

### Configuration

| File | Description |
|---|---|
| `config/EmbeddingConfig.java` | Spring `@Configuration` that creates two beans: `AllMiniLmL6V2EmbeddingModel` (converts text → 384-dimensional vectors) and `InMemoryEmbeddingStore` (holds vectors in memory for similarity search) |

### Models

| File | Description |
|---|---|
| `model/RagQueryRequest.java` | `record(String question)` — input for RAG queries |
| `model/RagQueryResponse.java` | `record(String answer, List<String> relevantChunks)` — RAG query result with the LLM answer and the retrieved log chunks that informed it |
| `model/IngestionResponse.java` | `record(int chunksIngested, String message)` — confirmation after log ingestion |

### Services

| File | Description |
|---|---|
| `service/LogIngestionService.java` | Ingests raw log text: splits into smart chunks, embeds each chunk, stores in the vector store |
| `service/RagService.java` | Full RAG pipeline: embeds the user's question, retrieves similar log chunks, augments a prompt with that context, and sends it to the LLM for a grounded answer |

### Controller

| File | Description |
|---|---|
| `controller/RagController.java` | REST endpoints under `/api/logs/rag/` for ingestion and querying |

### Tests

| File | Tests |
|---|---|
| `service/LogIngestionServiceTest.java` | 6 tests — chunking logic (single entry, stack trace preservation, grouping small entries, splitting large content) and ingestion (embed + store single chunk, embed + store multiple chunks) |
| `service/RagServiceTest.java` | 3 tests — full pipeline with results, context injection into prompt, no-match scenario |
| `controller/RagControllerTest.java` | 4 tests — ingest JSON, ingest multipart file, query with results, query with empty results |

---

## New API Endpoints

### `POST /api/logs/rag/ingest` (JSON)

Ingest log text into the vector store for later querying.

**Request:**
```json
{
  "logText": "2026-04-25 10:00:01 ERROR db Connection timeout after 30000ms\n2026-04-25 10:00:02 INFO app Retrying..."
}
```

**Response:**
```json
{
  "chunksIngested": 1,
  "message": "Ingested 1 chunks"
}
```

### `POST /api/logs/rag/ingest` (Multipart)

Upload a log file for ingestion.

**Request:** `multipart/form-data` with a `file` part containing the log file.

**Response:**
```json
{
  "chunksIngested": 5,
  "message": "Ingested 5 chunks from app.log"
}
```

### `POST /api/logs/rag/query`

Ask a natural-language question about previously ingested logs.

**Request:**
```json
{
  "question": "What database errors occurred?"
}
```

**Response:**
```json
{
  "answer": "There was a database connection timeout at 10:00:01...",
  "relevantChunks": [
    "2026-04-25 10:00:01 ERROR db Connection timeout after 30000ms"
  ]
}
```

---

## Smart Log Chunking Strategy

The `LogIngestionService` implements a log-aware chunking strategy (Day 11 of the roadmap):

1. **Split by log entry boundaries** — a new entry starts when a line begins with a timestamp (`YYYY-MM-DD HH:MM`). Lines without a timestamp (stack traces, continuation lines) are appended to the previous entry.
2. **Preserve stack traces** — multi-line exceptions stay together with their ERROR/FATAL line.
3. **Group into chunks** — small consecutive entries are grouped into chunks up to 1000 characters to balance retrieval granularity with context.

**Example:** Given this log input:
```
2026-04-25 10:00:01 ERROR db Connection failed
java.sql.SQLException: Connection refused
    at com.db.Pool.getConnection(Pool.java:42)
2026-04-25 10:00:02 INFO app Retrying
2026-04-25 10:00:03 INFO app Connected
```

The chunker produces entries where the stack trace stays attached to the ERROR line, and the two INFO lines are grouped together if they fit within the chunk size limit.

---

## RAG Pipeline Flow

```
User Question: "What database errors occurred?"
        │
        ▼
1. EMBED — AllMiniLmL6V2EmbeddingModel converts question to a 384-dim vector
        │
        ▼
2. RETRIEVE — InMemoryEmbeddingStore.findRelevant(vector, maxResults=5, minScore=0.5)
        │         returns the most semantically similar log chunks
        ▼
3. AUGMENT — Build a prompt injecting retrieved chunks as context:
        │     "Based on these logs: <chunks>... Answer: <question>"
        ▼
4. GENERATE — ChatLanguageModel (OpenAI GPT-4o) generates a grounded answer
        │
        ▼
RagQueryResponse { answer, relevantChunks }
```

---

## Backward Compatibility

**No existing files were modified.** All Phase 3 code is additive:

- `LangChain4jConfig.java` — unchanged
- `LogAnalysisAI.java` — unchanged
- `LogAnalysisService.java` — unchanged
- `LogAnalysisController.java` — unchanged
- All model classes — unchanged

### Test Verification

```
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

Existing Phase 2 tests:  21 (13 controller + 8 service) ✅
New Phase 3 tests:       13 (4 controller + 3 RAG service + 6 ingestion service) ✅
```

---

## Concepts Covered (Mapped to Roadmap)

| Day | Topic | Implementation |
|---|---|---|
| Day 7 | Embeddings & Semantic Similarity | `EmbeddingConfig` creates `AllMiniLmL6V2EmbeddingModel`; `LogIngestionService` embeds log chunks |
| Day 8–9 | Vector Databases & Storage | `InMemoryEmbeddingStore` stores and retrieves embeddings; `RagService.retrieve()` performs similarity search |
| Day 10 | RAG Architecture & Flow | `RagService.query()` implements the full Retrieve → Augment → Generate pipeline |
| Day 11 | Chunking Strategies | `LogIngestionService.chunkLogEntries()` implements log-aware chunking that preserves stack traces |
