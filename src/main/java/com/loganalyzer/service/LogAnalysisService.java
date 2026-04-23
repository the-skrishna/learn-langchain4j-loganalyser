package com.loganalyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.model.AnalysisMode;
import com.loganalyzer.model.ErrorDetectionResponse;
import com.loganalyzer.model.LogError;
import com.loganalyzer.model.RootCauseResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 1 Log Analysis Service
 *
 * Day 1: Direct LLM call with a plain user message.
 * Day 2: Temperature control + role-based prompt engineering.
 * Day 3: Specialized prompts — error detection, summarization, root cause analysis.
 */
@Service
public class LogAnalysisService {

    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LogAnalysisService(@Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    // Structured error detection — parses LLM JSON into typed objects
    public ErrorDetectionResponse detectErrorsStructured(String logText) {
        String raw = detectErrors(logText);
        try {
            String json = raw.replaceAll("(?s)```json\\s*|```", "").trim();
            List<LogError> errors = objectMapper.readValue(json, new TypeReference<>() {});
            return new ErrorDetectionResponse(errors.size(), errors);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse LLM error response: " + raw, e);
        }
    }

    // Structured root cause analysis — parses LLM JSON into typed RootCauseResponse
    public RootCauseResponse rootCauseAnalysisStructured(String logText) {
        String raw = rootCauseAnalysis(logText);
        try {
            String json = raw.replaceAll("(?s)```json\\s*|```", "").trim();
            return objectMapper.readValue(json, RootCauseResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse LLM root cause response: " + raw, e);
        }
    }

    // ── Day 1 ────────────────────────────────────────────────────────────────
    // Raw LLM call: no system prompt, default temperature.
    public String analyze(String logText) {
        OpenAiChatModel model = buildModel(0.0);
        return model.generate(logText);
    }

    // ── Day 2 ────────────────────────────────────────────────────────────────
    // Adds temperature control and a role-based system prompt.
    public String analyzeWithPrompt(String logText, double temperature) {
        OpenAiChatModel model = buildModel(temperature);

        String prompt = """
                You are a log analysis expert.
                Analyze the following log and identify any errors, warnings, or anomalies.
                Be concise and structured in your response.

                Log:
                %s
                """.formatted(logText);

        return model.generate(prompt);
    }

    // ── Day 3 ────────────────────────────────────────────────────────────────
    // Specialized prompts routed by AnalysisMode.
    public String analyzeWithMode(String logText, AnalysisMode mode) {
        return switch (mode) {
            case EXPLAIN -> analyzeWithPrompt(logText, 0.7);
            case DETECT_ERRORS -> detectErrors(logText);
            case SUMMARIZE -> summarize(logText);
            case ROOT_CAUSE -> rootCauseAnalysis(logText);
        };
    }

    // Day 3 — Error detection: deterministic, structured JSON output
    private String detectErrors(String logText) {
        OpenAiChatModel model = buildModel(0.0);

        String prompt = """
                You are an expert log analyzer. Identify all errors in the following log.

                An error includes:
                - ERROR or FATAL level messages
                - Exceptions and stack traces
                - Connection failures or timeouts

                Return results as a JSON array where each item has:
                  "lineNumber": integer,
                  "timestamp": string (or null if absent),
                  "level": string,
                  "message": string

                If no errors are found, return an empty array [].

                Log:
                %s
                """.formatted(logText);

        return model.generate(prompt);
    }

    // Day 3 — Summarization: brief, human-readable
    private String summarize(String logText) {
        OpenAiChatModel model = buildModel(0.0);

        String prompt = """
                You are a log analysis expert. Summarize the following log in 3-5 bullet points.
                Focus on: what happened, key errors or warnings, and overall system health.

                Log:
                %s
                """.formatted(logText);

        return model.generate(prompt);
    }

    // Day 3 — Root cause analysis: returns JSON parsed into RootCauseResponse
    private String rootCauseAnalysis(String logText) {
        OpenAiChatModel model = buildModel(0.0);

        String prompt = """
                You are a senior site reliability engineer. Perform a root cause analysis on the following log.

                Return only raw JSON (no markdown, no explanation) matching this structure:
                {
                  "observedSymptoms": ["symptom1", "symptom2"],
                  "mostLikelyRootCause": "single string explanation",
                  "contributingFactors": ["factor1", "factor2"],
                  "recommendedFixes": ["fix1", "fix2"]
                }

                Log:
                %s
                """.formatted(logText);

        return model.generate(prompt);
    }

    // ── Shared builder ────────────────────────────────────────────────────────
    private OpenAiChatModel buildModel(double temperature) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .temperature(temperature)
                .build();
    }
}
