package com.loganalyzer.controller;

import com.loganalyzer.model.IngestionResponse;
import com.loganalyzer.model.LogAnalysisRequest;
import com.loganalyzer.model.RagQueryRequest;
import com.loganalyzer.model.RagQueryResponse;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Phase 3 — RAG Controller
 *
 * New endpoints for log ingestion and RAG-powered querying.
 * Completely separate from LogAnalysisController — no existing APIs are modified.
 */
@RestController
@RequestMapping("/api/logs/rag")
public class RagController {

    private final LogIngestionService ingestionService;
    private final RagService ragService;

    public RagController(LogIngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    /**
     * Ingest log text into the vector store (JSON body).
     */
    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IngestionResponse ingest(@RequestBody LogAnalysisRequest request) {
        int chunks = ingestionService.ingest(request.logText());
        return new IngestionResponse(chunks, "Ingested " + chunks + " chunks");
    }

    /**
     * Ingest log file into the vector store (multipart upload).
     */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionResponse ingestFile(@RequestPart MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        int chunks = ingestionService.ingest(content);
        return new IngestionResponse(chunks, "Ingested " + chunks + " chunks from " + file.getOriginalFilename());
    }

    /**
     * Query ingested logs using RAG: retrieves relevant chunks and generates an answer.
     */
    @PostMapping("/query")
    public RagQueryResponse query(@RequestBody RagQueryRequest request) {
        RagService.RagResult result = ragService.query(request.question());
        return new RagQueryResponse(result.answer(), result.relevantChunks());
    }
}
