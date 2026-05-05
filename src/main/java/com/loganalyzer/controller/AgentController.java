package com.loganalyzer.controller;

import com.loganalyzer.ai.LogAnalysisAgent;
import com.loganalyzer.model.AgentQueryRequest;
import com.loganalyzer.model.AgentQueryResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 4 — Day 15: Agent Controller
 *
 * Exposes the autonomous agent via REST. The agent receives a natural-language
 * question, decides which tools to call (search, filter, analyze), executes
 * them, and returns a reasoned answer.
 *
 * Completely separate from existing controllers — no Phase 1-3 APIs are modified.
 */
@RestController
@RequestMapping("/api/logs/agent")
public class AgentController {

    private final LogAnalysisAgent agent;

    public AgentController(LogAnalysisAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/query")
    public AgentQueryResponse query(@RequestBody AgentQueryRequest request) {
        String answer = agent.chat(request.question());
        return new AgentQueryResponse(answer);
    }
}
