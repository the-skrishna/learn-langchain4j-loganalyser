package com.loganalyzer.controller;

import com.loganalyzer.model.AnalysisMode;
import com.loganalyzer.model.LogAnalysisRequest;
import com.loganalyzer.model.LogAnalysisResponse;
import com.loganalyzer.service.LogAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Phase 2 REST API — unchanged endpoints, simplified routing.
 *
 * The service layer now handles structured vs string responses internally,
 * so the controller just delegates without branching on mode.
 */
@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {

    private final LogAnalysisService service;

    public LogAnalysisController(LogAnalysisService service) {
        this.service = service;
    }

    // Day 1 — JSON
    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LogAnalysisResponse analyze(@RequestBody LogAnalysisRequest request) {
        return new LogAnalysisResponse(service.analyze(request.logText()));
    }

    // Day 1 — multipart
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponse analyzeMultipart(
            @RequestPart(required = false) String logText,
            @RequestPart(required = false) MultipartFile file) throws IOException {
        return new LogAnalysisResponse(service.analyze(resolveInput(logText, file)));
    }

    // Day 2 — JSON
    @PostMapping(value = "/analyze/prompt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LogAnalysisResponse analyzeWithPrompt(
            @RequestBody LogAnalysisRequest request,
            @RequestParam(defaultValue = "0.0") double temperature) {
        return new LogAnalysisResponse(service.analyzeWithPrompt(request.logText(), temperature));
    }

    // Day 2 — multipart
    @PostMapping(value = "/analyze/prompt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponse analyzeWithPromptMultipart(
            @RequestPart(required = false) String logText,
            @RequestPart(required = false) MultipartFile file,
            @RequestParam(defaultValue = "0.0") double temperature) throws IOException {
        return new LogAnalysisResponse(service.analyzeWithPrompt(resolveInput(logText, file), temperature));
    }

    // Day 3 — JSON (now delegates entirely to service.analyzeWithMode which returns structured types)
    @PostMapping(value = "/analyze/{mode}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object analyzeWithMode(
            @RequestBody LogAnalysisRequest request,
            @PathVariable AnalysisMode mode) {
        return service.analyzeWithMode(request.logText(), mode);
    }

    // Day 3 — multipart
    @PostMapping(value = "/analyze/{mode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object analyzeWithModeMultipart(
            @RequestPart(required = false) String logText,
            @RequestPart(required = false) MultipartFile file,
            @PathVariable AnalysisMode mode) throws IOException {
        return service.analyzeWithMode(resolveInput(logText, file), mode);
    }

    private String resolveInput(String logText, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        if (logText != null && !logText.isBlank()) {
            return logText;
        }
        throw new IllegalArgumentException("Provide either 'logText' or a 'file'");
    }
}
