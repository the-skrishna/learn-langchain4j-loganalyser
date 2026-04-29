package com.loganalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.model.LogAnalysisRequest;
import com.loganalyzer.model.RagQueryRequest;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogIngestionService ingestionService;

    @MockBean
    private RagService ragService;

    @Autowired
    private ObjectMapper objectMapper;

    // ── POST /api/logs/rag/ingest (JSON) ──

    @Test
    void ingest_json_returnsChunkCount() throws Exception {
        when(ingestionService.ingest(anyString())).thenReturn(3);

        mockMvc.perform(post("/api/logs/rag/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LogAnalysisRequest("2026-04-25 10:00:01 ERROR db Timeout"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunksIngested").value(3))
                .andExpect(jsonPath("$.message").value("Ingested 3 chunks"));
    }

    // ── POST /api/logs/rag/ingest (multipart) ──

    @Test
    void ingest_multipart_returnsChunkCount() throws Exception {
        when(ingestionService.ingest(anyString())).thenReturn(5);

        MockMultipartFile file = new MockMultipartFile(
                "file", "app.log", "text/plain", "log content".getBytes());

        mockMvc.perform(multipart("/api/logs/rag/ingest").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunksIngested").value(5))
                .andExpect(jsonPath("$.message").value("Ingested 5 chunks from app.log"));
    }

    // ── POST /api/logs/rag/query ──

    @Test
    void query_returnsAnswerAndChunks() throws Exception {
        when(ragService.query("What errors?")).thenReturn(
                new RagService.RagResult("DB timeout found", List.of("ERROR db Timeout")));

        mockMvc.perform(post("/api/logs/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RagQueryRequest("What errors?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("DB timeout found"))
                .andExpect(jsonPath("$.relevantChunks[0]").value("ERROR db Timeout"));
    }

    @Test
    void query_noResults_returnsEmptyChunks() throws Exception {
        when(ragService.query("unrelated")).thenReturn(
                new RagService.RagResult("No relevant logs found.", List.of()));

        mockMvc.perform(post("/api/logs/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RagQueryRequest("unrelated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("No relevant logs found."))
                .andExpect(jsonPath("$.relevantChunks").isEmpty());
    }
}
