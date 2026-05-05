package com.loganalyzer.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * Phase 4 — Day 15: Agent AI Service Interface
 *
 * Key concepts:
 * - This interface is wired with tools (LogTools) and chat memory, turning it
 *   into an autonomous agent that follows the Think → Act → Observe → Repeat loop.
 * - The LLM reads the user's question, decides which tool(s) to call, executes
 *   them, observes the results, and reasons about the final answer.
 * - Chat memory (MessageWindowChatMemory) enables multi-step reasoning within
 *   a single query — the agent remembers tool results across steps.
 */
public interface LogAnalysisAgent {

    @SystemMessage("""
            You are an intelligent log analysis agent. You have access to tools that can
            search, filter, and analyze ingested log data.

            When answering a question:
            1. Decide which tool(s) to use based on the question.
            2. Call the appropriate tool(s) to gather information.
            3. Analyze the tool results and provide a clear, concise answer.

            If no relevant logs are found, say so honestly.
            """)
    String chat(String userMessage);
}
