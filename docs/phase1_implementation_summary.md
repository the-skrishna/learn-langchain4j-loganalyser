# Phase 1 Implementation Summary — LLM Fundamentals & Basic AI Log Analyzer

## Project Setup

| Property | Value |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.4 |
| LangChain4j | 0.31.0 |
| LLM | OpenAI `gpt-4o` |
| Base package | `com.loganalyzer` |

### Key Dependencies (`pom.xml`)
- `langchain4j-open-ai-spring-boot-starter` — auto-configures `OpenAiChatModel` bean
- `langchain4j` — core abstractions
- `spring-boot-starter-web` — REST API

### Configuration (`application.yml`)
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}   # set via env var
      model-name: gpt-4o
      temperature: 0.0
```

---

## Day 1 — LLM Fundamentals: Raw API Call

**Endpoint:** `POST /api/logs/analyze`

**What it does:** Sends the raw log text directly to the LLM with no system prompt or parameters — the simplest possible call to understand how the API works.

**Key learning:** Tokens, context window, next-token prediction.

```json
// Request
{ "logText": "2024-01-15 ERROR DB connection refused at port 5432" }

// Response
{ "analysis": "The log shows a database connection failure..." }
```

**Code path:** `LogAnalysisController.analyze()` → `LogAnalysisService.analyze()` → `OpenAiChatModel.generate(logText)`

---

## Day 2 — LLM Parameters & Prompt Engineering

**Endpoint:** `POST /api/logs/analyze/prompt?temperature=0.7`

**What it does:** Adds two concepts:
1. **Temperature control** — caller passes `temperature` (0.0 = deterministic, 0.7 = creative)
2. **Role-based system prompt** — wraps the log in a structured instruction prompt

**Key learning:** How temperature changes output style; how prompt structure affects quality.

```
// temperature=0.0 → consistent, precise output (good for error detection)
// temperature=0.7 → more narrative, human-readable explanation
```

**Prompt used:**
```
You are a log analysis expert.
Analyze the following log and identify any errors, warnings, or anomalies.
Be concise and structured in your response.

Log: <logText>
```

---

## Day 3 — Specialized Prompts by Analysis Mode

**Endpoint:** `POST /api/logs/analyze/{mode}`

**Modes and their behaviour:**

| Mode | Temperature | Output style |
|---|---|---|
| `EXPLAIN` | 0.7 | Narrative explanation (reuses Day 2 prompt) |
| `DETECT_ERRORS` | 0.0 | JSON array of errors with lineNumber, timestamp, level, message |
| `SUMMARIZE` | 0.0 | 3-5 bullet points: what happened, key issues, system health |
| `ROOT_CAUSE` | 0.0 | Structured: symptoms → root cause → contributing factors → fix |

**Key learning:** Prompt design drives output format; low temperature is essential for structured/parseable output.

### Example — DETECT_ERRORS
```json
// POST /api/logs/analyze/DETECT_ERRORS
// Response
{
  "analysis": "[{\"lineNumber\":3,\"timestamp\":\"2024-01-15T10:23:01\",\"level\":\"ERROR\",\"message\":\"DB connection refused\"}]"
}
```

### Example — ROOT_CAUSE
```
1. Observed symptoms: Database connection refused on port 5432
2. Most likely root cause: PostgreSQL service is down or unreachable
3. Contributing factors: No retry logic, single DB host configured
4. Recommended fix: Add connection retry with exponential backoff; check DB health
```

---

## File Structure

```
log-analyzer/
├── pom.xml
└── src/main/
    ├── resources/
    │   └── application.yml
    └── java/com/loganalyzer/
        ├── LogAnalyzerApplication.java
        ├── controller/
        │   └── LogAnalysisController.java   ← 3 endpoints (Day 1, 2, 3)
        ├── model/
        │   ├── AnalysisMode.java            ← EXPLAIN | DETECT_ERRORS | SUMMARIZE | ROOT_CAUSE
        │   ├── LogAnalysisRequest.java      ← record { logText }
        │   └── LogAnalysisResponse.java     ← record { analysis }
        └── service/
            └── LogAnalysisService.java      ← all prompt logic
```

---

## How to Run

```bash
export OPENAI_API_KEY=sk-...
cd log-analyzer
mvn spring-boot:run
```

### Quick test
```bash
# Day 1
curl -X POST http://localhost:8080/api/logs/analyze \
  -H "Content-Type: application/json" \
  -d '{"logText":"ERROR: NullPointerException at line 42"}'

# Day 2 — creative explanation
curl -X POST "http://localhost:8080/api/logs/analyze/prompt?temperature=0.7" \
  -H "Content-Type: application/json" \
  -d '{"logText":"ERROR: NullPointerException at line 42"}'

# Day 3 — root cause
curl -X POST http://localhost:8080/api/logs/analyze/ROOT_CAUSE \
  -H "Content-Type: application/json" \
  -d '{"logText":"ERROR: NullPointerException at line 42"}'
```

---

## Phase 1 Deliverable ✅

Working REST API that accepts log text and returns AI analysis using well-crafted prompts — covering raw LLM calls, temperature tuning, and specialized prompt design.

**Next: Phase 2** — Refactor to LangChain4j `ChatLanguageModel`, `PromptTemplate`, and AI Services for cleaner architecture and structured outputs.
