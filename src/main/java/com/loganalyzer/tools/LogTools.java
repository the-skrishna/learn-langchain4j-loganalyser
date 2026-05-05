package com.loganalyzer.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Phase 4 — Days 12-14: Tool-annotated methods for log operations.
 *
 * Key concepts:
 * - @Tool: Marks a method as a tool the LLM can invoke autonomously.
 *   The annotation value is the tool description — the LLM reads it to decide
 *   when to call this tool.
 * - @P: Describes each parameter so the LLM knows what to pass.
 * - The LLM decides WHICH tool to call and with WHAT arguments based on the
 *   user's natural-language query. We never call these methods directly.
 *
 * Day 14 principles applied:
 * - All tools are idempotent (read-only queries against the vector store).
 * - Clear descriptions help the LLM choose the right tool.
 * - Input validation prevents bad data from reaching the store.
 */
@Component
public class LogTools {

    private static final Logger log = LoggerFactory.getLogger(LogTools.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public LogTools(EmbeddingModel embeddingModel,
                    EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Tool("Search ingested logs by keyword or semantic meaning. Returns matching log entries.")
    public List<String> searchLogs(
            @P("keyword or phrase to search for in logs") String keyword,
            @P("maximum number of results to return") int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of("No keyword provided.");
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        log.info("Tool searchLogs called: keyword='{}', limit={}", keyword, safeLimit);

        Embedding queryEmbedding = embeddingModel.embed(keyword).content();
        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore.findRelevant(queryEmbedding, safeLimit, 0.3);

        return matches.stream()
                .map(m -> m.embedded().text())
                .toList();
    }

    @Tool("Filter ingested logs by log level such as ERROR, WARN, INFO, or DEBUG. Returns matching entries.")
    public List<String> filterByLevel(
            @P("log level to filter by, e.g. ERROR, WARN, INFO, DEBUG") String level) {
        if (level == null || level.isBlank()) {
            return List.of("No level provided.");
        }
        log.info("Tool filterByLevel called: level='{}'", level);

        // Semantic search for the level keyword to find relevant chunks
        Embedding queryEmbedding = embeddingModel.embed(level + " level log entries").content();
        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore.findRelevant(queryEmbedding, 20, 0.3);

        Pattern levelPattern = Pattern.compile("\\b" + Pattern.quote(level.toUpperCase()) + "\\b");
        return matches.stream()
                .map(m -> m.embedded().text())
                .filter(text -> levelPattern.matcher(text.toUpperCase()).find())
                .toList();
    }

    @Tool("Analyze error patterns in ingested logs. Returns a map of error message patterns to their occurrence counts.")
    public Map<String, Integer> analyzeErrorPatterns() {
        log.info("Tool analyzeErrorPatterns called");

        Embedding queryEmbedding = embeddingModel.embed("ERROR FATAL exception failure").content();
        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore.findRelevant(queryEmbedding, 50, 0.3);

        Pattern errorLine = Pattern.compile("(?i)(ERROR|FATAL)\\s+\\S+\\s+(.+)");
        return matches.stream()
                .map(m -> m.embedded().text())
                .flatMap(text -> text.lines()
                        .map(line -> errorLine.matcher(line))
                        .filter(java.util.regex.Matcher::find)
                        .map(m -> m.group(2).trim()))
                .collect(Collectors.groupingBy(
                        msg -> msg,
                        Collectors.summingInt(e -> 1)));
    }
}
