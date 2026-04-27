package com.loganalyzer.ai;

import com.loganalyzer.model.ErrorDetectionResponse;
import com.loganalyzer.model.RootCauseResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Phase 2 — Day 5: AI Service Interface
 *
 * Key concept: Interface-driven AI. You define WHAT you want (the interface),
 * and LangChain4j handles HOW (prompt construction, LLM call, response parsing).
 *
 * How it works:
 * 1. @SystemMessage → sets the LLM's role/persona (sent as a system message)
 * 2. @UserMessage   → the prompt template with {{variables}} (sent as a user message)
 * 3. @V("name")     → binds method parameters to template variables
 * 4. Return type     → LangChain4j auto-parses LLM output into this type
 *
 * Day 6 addition: Returning typed objects (ErrorDetectionResponse, RootCauseResponse)
 * instead of raw Strings. LangChain4j instructs the LLM to return JSON matching
 * the Java type and deserializes it automatically — no manual ObjectMapper needed.
 */
public interface LogAnalysisAI {

    @SystemMessage("You are a log analysis expert. Be concise and structured.")
    @UserMessage("""
            Analyze the following log and identify any errors, warnings, or anomalies.

            Log:
            {{logContent}}
            """)
    String analyze(@V("logContent") String logContent);

    @SystemMessage("You are an expert log analyzer.")
    @UserMessage("""
            Identify all errors in the following log.

            An error includes:
            - ERROR or FATAL level messages
            - Exceptions and stack traces
            - Connection failures or timeouts

            Log:
            {{logContent}}
            """)
    ErrorDetectionResponse detectErrors(@V("logContent") String logContent);

    @SystemMessage("You are a log analysis expert.")
    @UserMessage("""
            Summarize the following log in 3-5 bullet points.
            Focus on: what happened, key errors or warnings, and overall system health.

            Log:
            {{logContent}}
            """)
    String summarize(@V("logContent") String logContent);

    @SystemMessage("You are a senior site reliability engineer.")
    @UserMessage("""
            Perform a root cause analysis on the following log.

            Log:
            {{logContent}}
            """)
    RootCauseResponse rootCauseAnalysis(@V("logContent") String logContent);
}
