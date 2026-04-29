package com.loganalyzer.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 3 — Day 7-9: Embedding & Vector Store Configuration
 *
 * Key concepts:
 * - EmbeddingModel: Converts text into vector representations (embeddings).
 *   AllMiniLmL6V2 runs in-process via ONNX — no API key or external service needed.
 * - InMemoryEmbeddingStore: Simple vector store that holds embeddings in memory.
 *   Good for learning; swap to Chroma/Pinecone for production.
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
