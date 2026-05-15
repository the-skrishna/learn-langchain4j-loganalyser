package com.loganalyzer.controller;

import com.loganalyzer.ai.ConversationalAgent;
import com.loganalyzer.model.ChatRequest;
import com.loganalyzer.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 5 — Day 18: Conversational Controller
 *
 * Session-based multi-turn conversations. Each sessionId gets isolated memory,
 * enabling follow-up questions like:
 *   "Show errors" → "Filter by database" → "What caused them?"
 *
 * The ConversationalAgent uses @MemoryId to route each session to its own
 * MessageWindowChatMemory instance.
 */
@RestController
@RequestMapping("/api/logs/conversation")
public class ConversationController {

    private final ConversationalAgent conversationalAgent;

    public ConversationController(ConversationalAgent conversationalAgent) {
        this.conversationalAgent = conversationalAgent;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = conversationalAgent.chat(request.sessionId(), request.message());
        return new ChatResponse(request.sessionId(), answer);
    }
}
