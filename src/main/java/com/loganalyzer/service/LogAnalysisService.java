package com.loganalyzer.service;

import com.loganalyzer.ai.LogAnalysisAI;
import com.loganalyzer.model.AnalysisMode;
import com.loganalyzer.model.ErrorDetectionResponse;
import com.loganalyzer.model.RootCauseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Phase 2 — Refactored Log Analysis Service
 *
 * What changed from Phase 1:
 * - No more manual OpenAiChatModel creation (buildModel is gone)
 * - No more String.formatted() prompt construction
 * - No more ObjectMapper JSON parsing
 * - All of that is handled by the LogAnalysisAI interface + LangChain4j
 *
 * This service is now a thin orchestration layer:
 * Controller → Service (routing + error handling) → AI Service (LLM interaction)
 */
@Service
public class LogAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisService.class);
    private final LogAnalysisAI logAnalysisAI;

    public LogAnalysisService(LogAnalysisAI logAnalysisAI) {
        this.logAnalysisAI = logAnalysisAI;
    }

    public String analyze(String logText) {
        return logAnalysisAI.analyze(logText);
    }

    public String analyzeWithPrompt(String logText, double temperature) {
        // Temperature is now configured centrally in LangChain4jConfig.
        // The AI Service uses the shared ChatLanguageModel bean.
        return logAnalysisAI.analyze(logText);
    }

    public Object analyzeWithMode(String logText, AnalysisMode mode) {
        return switch (mode) {
            case EXPLAIN -> logAnalysisAI.analyze(logText);
            case DETECT_ERRORS -> detectErrorsStructured(logText);
            case SUMMARIZE -> logAnalysisAI.summarize(logText);
            case ROOT_CAUSE -> rootCauseAnalysisStructured(logText);
        };
    }

    /**
     * Day 6: Structured output — LangChain4j parses JSON into ErrorDetectionResponse automatically.
     * We wrap in try-catch for graceful error handling if the LLM returns malformed output.
     */
    public ErrorDetectionResponse detectErrorsStructured(String logText) {
        try {
            return logAnalysisAI.detectErrors(logText);
        } catch (Exception e) {
            log.error("Failed to parse structured error detection response", e);
            throw new IllegalStateException("AI returned an unparseable response for error detection", e);
        }
    }

    public RootCauseResponse rootCauseAnalysisStructured(String logText) {
        try {
            return logAnalysisAI.rootCauseAnalysis(logText);
        } catch (Exception e) {
            log.error("Failed to parse structured root cause response", e);
            throw new IllegalStateException("AI returned an unparseable response for root cause analysis", e);
        }
    }
}
