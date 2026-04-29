package com.loganalyzer.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogIngestionServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private LogIngestionService service;

    private static final Embedding DUMMY_EMBEDDING = Embedding.from(new float[]{0.1f, 0.2f});

    // ── chunking tests ──

    @Test
    void chunkLogEntries_singleEntry() {
        List<String> chunks = service.chunkLogEntries("2026-04-25 10:00:01 ERROR db Timeout");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("ERROR db Timeout");
    }

    @Test
    void chunkLogEntries_preservesStackTrace() {
        String log = """
                2026-04-25 10:00:01 ERROR db Connection failed
                java.sql.SQLException: Connection refused
                    at com.db.Pool.getConnection(Pool.java:42)
                    at com.app.Service.query(Service.java:15)
                2026-04-25 10:00:02 INFO app Retrying""";

        List<String> chunks = service.chunkLogEntries(log);
        // The stack trace should stay with the ERROR entry
        String errorChunk = chunks.stream()
                .filter(c -> c.contains("Connection failed"))
                .findFirst().orElseThrow();
        assertThat(errorChunk).contains("SQLException");
        assertThat(errorChunk).contains("Pool.java:42");
    }

    @Test
    void chunkLogEntries_groupsSmallEntries() {
        String log = """
                2026-04-25 10:00:01 INFO a Short
                2026-04-25 10:00:02 INFO b Short
                2026-04-25 10:00:03 INFO c Short""";

        List<String> chunks = service.chunkLogEntries(log);
        // All 3 short entries should fit in one chunk (well under 1000 chars)
        assertThat(chunks).hasSize(1);
    }

    @Test
    void chunkLogEntries_splitsLargeContent() {
        // Create entries that exceed MAX_CHUNK_CHARS when combined
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            log.append("2026-04-25 10:00:%02d INFO app %s\n".formatted(i, "x".repeat(80)));
        }
        List<String> chunks = service.chunkLogEntries(log.toString());
        assertThat(chunks.size()).isGreaterThan(1);
    }

    // ── ingest tests ──

    @Test
    void ingest_embedsAndStoresChunks() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY_EMBEDDING));

        String log = "2026-04-25 10:00:01 ERROR db Timeout";
        int count = service.ingest(log);

        assertThat(count).isEqualTo(1);
        verify(embeddingModel).embed(anyString());
        verify(embeddingStore).add(any(Embedding.class), any(TextSegment.class));
    }

    @Test
    void ingest_multipleChunks_embedsEach() {
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(DUMMY_EMBEDDING));

        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            log.append("2026-04-25 10:00:%02d INFO app %s\n".formatted(i, "x".repeat(80)));
        }
        int count = service.ingest(log.toString());

        assertThat(count).isGreaterThan(1);
        verify(embeddingModel, times(count)).embed(anyString());
        verify(embeddingStore, times(count)).add(any(Embedding.class), any(TextSegment.class));
    }
}
