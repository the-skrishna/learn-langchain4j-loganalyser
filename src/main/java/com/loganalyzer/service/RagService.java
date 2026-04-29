package com.loganalyzer.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 3 — Day 10: RAG (Retrieval Augmented Generation) Service
 *
 * RAG flow:
 * 1. Query  — embed the user's question
 * 2. Retrieve — find the most similar log chunks from the vector store
 * 3. Augment — inject retrieved context into the prompt
 * 4. Generate — send augmented prompt to the LLM for a grounded answer
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.5;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatLanguageModel;

    public RagService(EmbeddingModel embeddingModel,
                      EmbeddingStore<TextSegment> embeddingStore,
                      ChatLanguageModel chatLanguageModel) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * Full RAG pipeline: retrieve relevant log chunks, augment prompt, generate answer.
     */
    public RagResult query(String question) {
        // 1. Retrieve
        List<EmbeddingMatch<TextSegment>> matches = retrieve(question);
        List<String> relevantChunks = matches.stream()
                .map(m -> m.embedded().text())
                .toList();

        // 2. Augment + 3. Generate
        String context = relevantChunks.stream().collect(Collectors.joining("\n---\n"));
        String prompt = """
                You are a log analysis expert. Answer the question based ONLY on the provided log context.
                If the logs don't contain enough information, say so.
                
                Log context:
                %s
                
                Question: %s
                """.formatted(context, question);

        String answer = chatLanguageModel.generate(prompt);
        log.info("RAG query completed: {} chunks retrieved for question: {}", relevantChunks.size(), question);
        return new RagResult(answer, relevantChunks);
    }

    /**
     * Retrieve the most relevant log chunks for a given question.
     */
    List<EmbeddingMatch<TextSegment>> retrieve(String question) {
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        return embeddingStore.findRelevant(questionEmbedding, MAX_RESULTS, MIN_SCORE);
    }

    public record RagResult(String answer, List<String> relevantChunks) {}
}
