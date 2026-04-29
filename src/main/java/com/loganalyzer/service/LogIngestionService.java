package com.loganalyzer.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Phase 3 — Day 11: Log Ingestion Service
 *
 * Smart chunking strategy for logs:
 * - Splits by log entry boundaries (timestamp-prefixed lines)
 * - Keeps multi-line entries together (stack traces stay with their ERROR line)
 * - Groups small consecutive entries into chunks up to a max size
 */
@Service
public class LogIngestionService {

    private static final Logger log = LoggerFactory.getLogger(LogIngestionService.class);

    // Matches common log timestamps: 2026-04-25 10:00:01, 2026-04-25T10:00:01, etc.
    private static final Pattern LOG_LINE_START = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}");

    private static final int MAX_CHUNK_CHARS = 1000;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public LogIngestionService(EmbeddingModel embeddingModel,
                               EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Ingest raw log text: chunk smartly, embed, and store.
     * @return number of chunks ingested
     */
    public int ingest(String logContent) {
        List<String> chunks = chunkLogEntries(logContent);
        for (String chunk : chunks) {
            TextSegment segment = TextSegment.from(chunk);
            Embedding embedding = embeddingModel.embed(segment.text()).content();
            embeddingStore.add(embedding, segment);
        }
        log.info("Ingested {} chunks into vector store", chunks.size());
        return chunks.size();
    }

    /**
     * Smart chunking: split log into individual entries (preserving stack traces),
     * then group entries into chunks up to MAX_CHUNK_CHARS.
     */
    List<String> chunkLogEntries(String logContent) {
        List<String> entries = splitIntoLogEntries(logContent);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String entry : entries) {
            if (current.length() + entry.length() > MAX_CHUNK_CHARS && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(entry).append("\n");
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    /**
     * Split raw log text into logical entries. A new entry starts when a line
     * begins with a timestamp. Lines without a timestamp (e.g., stack trace
     * continuation) are appended to the previous entry.
     */
    private List<String> splitIntoLogEntries(String logContent) {
        String[] lines = logContent.split("\n");
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (LOG_LINE_START.matcher(line).find() && !current.isEmpty()) {
                entries.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(line).append("\n");
        }
        if (!current.isEmpty()) {
            entries.add(current.toString().trim());
        }
        return entries;
    }
}
