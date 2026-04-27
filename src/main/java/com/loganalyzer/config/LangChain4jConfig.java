package com.loganalyzer.config;

import com.loganalyzer.ai.LogAnalysisAI;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 2 — Day 4 & 5: LangChain4j Configuration
 *
 * Key concepts:
 * - ChatLanguageModel: LangChain4j's abstraction over any LLM provider.
 *   Code to the interface, swap providers without changing business logic.
 * - AiServices.create(): Generates a proxy implementation of your AI interface.
 *   It reads @SystemMessage/@UserMessage annotations, constructs prompts,
 *   calls the LLM, and parses the response into your return type.
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.temperature:0.0}") double temperature) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Day 5: AiServices creates a proxy that implements LogAnalysisAI.
     * Each method call becomes: build prompt → call LLM → parse response.
     */
    @Bean
    public LogAnalysisAI logAnalysisAI(ChatLanguageModel chatLanguageModel) {
        return AiServices.create(LogAnalysisAI.class, chatLanguageModel);
    }
}
