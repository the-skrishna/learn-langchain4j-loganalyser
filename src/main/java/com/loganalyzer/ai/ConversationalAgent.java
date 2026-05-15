package com.loganalyzer.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Phase 5 — Days 16-18: Conversational Agent with Per-Session Memory
 *
 * Key concepts:
 * - @MemoryId: Each unique sessionId gets its own isolated chat memory.
 *   This enables multi-turn conversations where follow-up questions like
 *   "Filter those by database" or "What caused them?" work correctly.
 * - ChatMemoryProvider: Creates a new MessageWindowChatMemory for each session.
 *   Configured in LangChain4jConfig.
 * - Tools are still available — the conversational agent can search, filter,
 *   and analyze logs while maintaining conversation context.
 */
public interface ConversationalAgent {

    @SystemMessage("""
            You are an intelligent log analysis assistant. You have access to tools that can
            search, filter, and analyze ingested log data.

            You maintain conversation context across messages. When the user says
            "those", "them", "it", or refers to previous results, use the conversation
            history to understand what they mean.

            When answering:
            1. Use tools to gather information when needed.
            2. Remember previous results and build on them.
            3. Be concise and structured in your responses.
            """)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
