package com.loganalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.model.*;
import com.loganalyzer.service.LogAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogAnalysisController.class)
class LogAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogAnalysisService service;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SAMPLE_LOG = "2026-04-25 10:00:01 ERROR db Connection timeout";

    // ── /api/logs/analyze (JSON) ──

    @Test
    void analyze_json_returnsAnalysis() throws Exception {
        when(service.analyze(SAMPLE_LOG)).thenReturn("Found 1 error");

        mockMvc.perform(post("/api/logs/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("Found 1 error"));
    }

    // ── /api/logs/analyze (multipart — file) ──

    @Test
    void analyze_multipart_withFile() throws Exception {
        when(service.analyze(SAMPLE_LOG)).thenReturn("File analysis result");

        MockMultipartFile file = new MockMultipartFile("file", "app.log", "text/plain", SAMPLE_LOG.getBytes());

        mockMvc.perform(multipart("/api/logs/analyze").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("File analysis result"));
    }

    // ── /api/logs/analyze (multipart — logText) ──

    @Test
    void analyze_multipart_withLogText() throws Exception {
        when(service.analyze(SAMPLE_LOG)).thenReturn("Text analysis result");

        MockMultipartFile logText = new MockMultipartFile("logText", "", "text/plain", SAMPLE_LOG.getBytes());

        mockMvc.perform(multipart("/api/logs/analyze").file(logText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("Text analysis result"));
    }

    // ── /api/logs/analyze (multipart — neither) ──

    @Test
    void analyze_multipart_noInput_throws() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(multipart("/api/logs/analyze")));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    // ── /api/logs/analyze/prompt (JSON) ──

    @Test
    void analyzeWithPrompt_json_defaultTemperature() throws Exception {
        when(service.analyzeWithPrompt(eq(SAMPLE_LOG), eq(0.0))).thenReturn("Prompt result");

        mockMvc.perform(post("/api/logs/analyze/prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("Prompt result"));
    }

    @Test
    void analyzeWithPrompt_json_customTemperature() throws Exception {
        when(service.analyzeWithPrompt(eq(SAMPLE_LOG), eq(0.7))).thenReturn("Creative result");

        mockMvc.perform(post("/api/logs/analyze/prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("temperature", "0.7")
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("Creative result"));
    }

    // ── /api/logs/analyze/prompt (multipart) ──

    @Test
    void analyzeWithPrompt_multipart_withFile() throws Exception {
        when(service.analyzeWithPrompt(eq(SAMPLE_LOG), eq(0.0))).thenReturn("Multipart prompt result");

        MockMultipartFile file = new MockMultipartFile("file", "app.log", "text/plain", SAMPLE_LOG.getBytes());

        mockMvc.perform(multipart("/api/logs/analyze/prompt").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("Multipart prompt result"));
    }

    // ── /api/logs/analyze/{mode} (JSON — all 4 modes) ──

    @Test
    void analyzeWithMode_explain() throws Exception {
        when(service.analyzeWithMode(SAMPLE_LOG, AnalysisMode.EXPLAIN)).thenReturn("Explanation");

        mockMvc.perform(post("/api/logs/analyze/EXPLAIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(content().string("Explanation"));
    }

    @Test
    void analyzeWithMode_detectErrors() throws Exception {
        var response = new ErrorDetectionResponse(1, List.of(
                new LogError(1, "2026-04-25 10:00:01", "ERROR", "Connection timeout")));
        when(service.analyzeWithMode(SAMPLE_LOG, AnalysisMode.DETECT_ERRORS)).thenReturn(response);

        mockMvc.perform(post("/api/logs/analyze/DETECT_ERRORS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalErrors").value(1))
                .andExpect(jsonPath("$.errors[0].level").value("ERROR"));
    }

    @Test
    void analyzeWithMode_summarize() throws Exception {
        when(service.analyzeWithMode(SAMPLE_LOG, AnalysisMode.SUMMARIZE)).thenReturn("Summary");

        mockMvc.perform(post("/api/logs/analyze/SUMMARIZE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(content().string("Summary"));
    }

    @Test
    void analyzeWithMode_rootCause() throws Exception {
        var response = new RootCauseResponse(
                List.of("Connection timeout"), "Database overloaded",
                List.of("High traffic"), List.of("Scale DB pool"));
        when(service.analyzeWithMode(SAMPLE_LOG, AnalysisMode.ROOT_CAUSE)).thenReturn(response);

        mockMvc.perform(post("/api/logs/analyze/ROOT_CAUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostLikelyRootCause").value("Database overloaded"))
                .andExpect(jsonPath("$.recommendedFixes[0]").value("Scale DB pool"));
    }

    @Test
    void analyzeWithMode_invalidMode_returns400() throws Exception {
        mockMvc.perform(post("/api/logs/analyze/INVALID_MODE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogAnalysisRequest(SAMPLE_LOG))))
                .andExpect(status().isBadRequest());
    }

    // ── /api/logs/analyze/{mode} (multipart) ──

    @Test
    void analyzeWithMode_multipart_withFile() throws Exception {
        when(service.analyzeWithMode(SAMPLE_LOG, AnalysisMode.SUMMARIZE)).thenReturn("File summary");

        MockMultipartFile file = new MockMultipartFile("file", "app.log", "text/plain", SAMPLE_LOG.getBytes());

        mockMvc.perform(multipart("/api/logs/analyze/SUMMARIZE").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("File summary"));
    }
}
