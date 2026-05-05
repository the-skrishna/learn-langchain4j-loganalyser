package com.loganalyzer.tools;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogToolsTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private LogTools logTools;

    private static final Embedding DUMMY = Embedding.from(new float[]{0.1f, 0.2f});

    // ── searchLogs ──

    @Test
    void searchLogs_returnsMatchingEntries() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(5), eq(0.3)))
                .thenReturn(List.of(
                        new EmbeddingMatch<>(0.9, "id1", DUMMY,
                                TextSegment.from("ERROR db Connection timeout"))));

        List<String> results = logTools.searchLogs("timeout", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("Connection timeout");
    }

    @Test
    void searchLogs_blankKeyword_returnsMessage() {
        List<String> results = logTools.searchLogs("", 5);
        assertThat(results).containsExactly("No keyword provided.");
    }

    @Test
    void searchLogs_clampsLimit() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(20), eq(0.3)))
                .thenReturn(List.of());

        // limit > 20 should be clamped to 20
        List<String> results = logTools.searchLogs("test", 100);
        assertThat(results).isEmpty();
    }

    // ── filterByLevel ──

    @Test
    void filterByLevel_returnsOnlyMatchingLevel() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(20), eq(0.3)))
                .thenReturn(List.of(
                        new EmbeddingMatch<>(0.9, "id1", DUMMY,
                                TextSegment.from("2026-04-25 10:00:01 ERROR db Timeout")),
                        new EmbeddingMatch<>(0.8, "id2", DUMMY,
                                TextSegment.from("2026-04-25 10:00:02 INFO app Started"))));

        List<String> results = logTools.filterByLevel("ERROR");

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("ERROR");
    }

    @Test
    void filterByLevel_blankLevel_returnsMessage() {
        List<String> results = logTools.filterByLevel("");
        assertThat(results).containsExactly("No level provided.");
    }

    // ── analyzeErrorPatterns ──

    @Test
    void analyzeErrorPatterns_groupsByMessage() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(50), eq(0.3)))
                .thenReturn(List.of(
                        new EmbeddingMatch<>(0.9, "id1", DUMMY,
                                TextSegment.from("2026-04-25 10:00:01 ERROR db Connection timeout\n2026-04-25 10:00:02 ERROR db Connection timeout")),
                        new EmbeddingMatch<>(0.8, "id2", DUMMY,
                                TextSegment.from("2026-04-25 10:00:03 ERROR app NullPointerException"))));

        Map<String, Integer> patterns = logTools.analyzeErrorPatterns();

        assertThat(patterns).containsEntry("Connection timeout", 2);
        assertThat(patterns).containsEntry("NullPointerException", 1);
    }

    @Test
    void analyzeErrorPatterns_noErrors_returnsEmptyMap() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(50), eq(0.3)))
                .thenReturn(List.of(
                        new EmbeddingMatch<>(0.8, "id1", DUMMY,
                                TextSegment.from("2026-04-25 10:00:01 INFO app All good"))));

        Map<String, Integer> patterns = logTools.analyzeErrorPatterns();

        assertThat(patterns).isEmpty();
    }
}
