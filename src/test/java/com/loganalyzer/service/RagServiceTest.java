package com.loganalyzer.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private RagService ragService;

    private static final Embedding DUMMY_EMBEDDING = Embedding.from(new float[]{0.1f, 0.2f});

    @Test
    void query_retrievesChunksAndGeneratesAnswer() {
        // Setup: embedding model returns a dummy embedding
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY_EMBEDDING));

        // Setup: vector store returns matching chunks
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                0.9, "id1", DUMMY_EMBEDDING,
                TextSegment.from("2026-04-25 10:00:03 ERROR db Connection timeout"));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(5), eq(0.5)))
                .thenReturn(List.of(match));

        // Setup: LLM returns an answer
        when(chatLanguageModel.generate(anyString())).thenReturn("The database connection timed out.");

        // Execute
        RagService.RagResult result = ragService.query("What database errors occurred?");

        // Verify
        assertThat(result.answer()).isEqualTo("The database connection timed out.");
        assertThat(result.relevantChunks()).hasSize(1);
        assertThat(result.relevantChunks().get(0)).contains("Connection timeout");
    }

    @Test
    void query_includesContextInPrompt() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY_EMBEDDING));

        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                0.8, "id1", DUMMY_EMBEDDING,
                TextSegment.from("ERROR: NullPointerException"));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(5), eq(0.5)))
                .thenReturn(List.of(match));
        when(chatLanguageModel.generate(anyString())).thenReturn("answer");

        ragService.query("What errors?");

        // Capture the prompt sent to the LLM and verify it contains the retrieved context
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatLanguageModel).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("NullPointerException");
        assertThat(promptCaptor.getValue()).contains("What errors?");
    }

    @Test
    void query_noMatchesReturnsEmptyChunks() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY_EMBEDDING));
        when(embeddingStore.findRelevant(any(Embedding.class), eq(5), eq(0.5)))
                .thenReturn(List.of());
        when(chatLanguageModel.generate(anyString())).thenReturn("No relevant logs found.");

        RagService.RagResult result = ragService.query("Something unrelated");

        assertThat(result.relevantChunks()).isEmpty();
        assertThat(result.answer()).isEqualTo("No relevant logs found.");
    }
}
