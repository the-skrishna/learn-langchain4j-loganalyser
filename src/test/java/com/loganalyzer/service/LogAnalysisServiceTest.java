package com.loganalyzer.service;

import com.loganalyzer.ai.LogAnalysisAI;
import com.loganalyzer.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAnalysisServiceTest {

    @Mock
    private LogAnalysisAI logAnalysisAI;

    @InjectMocks
    private LogAnalysisService service;

    private static final String LOG = "2026-04-25 10:00:03 ERROR db Connection timeout";

    // ── analyze ──

    @Test
    void analyze_delegatesToAI() {
        when(logAnalysisAI.analyze(LOG)).thenReturn("Found 1 error");
        assertThat(service.analyze(LOG)).isEqualTo("Found 1 error");
    }

    // ── analyzeWithPrompt ──

    @Test
    void analyzeWithPrompt_ignoresTemperature_delegatesToAI() {
        when(logAnalysisAI.analyze(LOG)).thenReturn("Result");
        // temperature param is accepted but not used — service always delegates to AI with shared config
        assertThat(service.analyzeWithPrompt(LOG, 0.7)).isEqualTo("Result");
    }

    // ── analyzeWithMode — EXPLAIN ──

    @Test
    void analyzeWithMode_explain_returnsString() {
        when(logAnalysisAI.analyze(LOG)).thenReturn("Explanation");
        Object result = service.analyzeWithMode(LOG, AnalysisMode.EXPLAIN);
        assertThat(result).isEqualTo("Explanation");
    }

    // ── analyzeWithMode — SUMMARIZE ──

    @Test
    void analyzeWithMode_summarize_returnsString() {
        when(logAnalysisAI.summarize(LOG)).thenReturn("• 1 error found");
        Object result = service.analyzeWithMode(LOG, AnalysisMode.SUMMARIZE);
        assertThat(result).isEqualTo("• 1 error found");
    }

    // ── analyzeWithMode — DETECT_ERRORS ──

    @Test
    void analyzeWithMode_detectErrors_returnsStructured() {
        var expected = new ErrorDetectionResponse(1, List.of(
                new LogError(1, "2026-04-25 10:00:03", "ERROR", "Connection timeout")));
        when(logAnalysisAI.detectErrors(LOG)).thenReturn(expected);

        Object result = service.analyzeWithMode(LOG, AnalysisMode.DETECT_ERRORS);
        assertThat(result).isInstanceOf(ErrorDetectionResponse.class);
        ErrorDetectionResponse response = (ErrorDetectionResponse) result;
        assertThat(response.totalErrors()).isEqualTo(1);
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).level()).isEqualTo("ERROR");
    }

    // ── analyzeWithMode — ROOT_CAUSE ──

    @Test
    void analyzeWithMode_rootCause_returnsStructured() {
        var expected = new RootCauseResponse(
                List.of("Timeout"), "DB overloaded",
                List.of("High traffic"), List.of("Scale pool"));
        when(logAnalysisAI.rootCauseAnalysis(LOG)).thenReturn(expected);

        Object result = service.analyzeWithMode(LOG, AnalysisMode.ROOT_CAUSE);
        assertThat(result).isInstanceOf(RootCauseResponse.class);
        RootCauseResponse response = (RootCauseResponse) result;
        assertThat(response.mostLikelyRootCause()).isEqualTo("DB overloaded");
        assertThat(response.recommendedFixes()).containsExactly("Scale pool");
    }

    // ── detectErrorsStructured — error handling ──

    @Test
    void detectErrorsStructured_aiThrows_wrapsInIllegalState() {
        when(logAnalysisAI.detectErrors(LOG)).thenThrow(new RuntimeException("Malformed JSON"));

        assertThatThrownBy(() -> service.detectErrorsStructured(LOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unparseable response for error detection")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    // ── rootCauseAnalysisStructured — error handling ──

    @Test
    void rootCauseAnalysisStructured_aiThrows_wrapsInIllegalState() {
        when(logAnalysisAI.rootCauseAnalysis(LOG)).thenThrow(new RuntimeException("Malformed JSON"));

        assertThatThrownBy(() -> service.rootCauseAnalysisStructured(LOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unparseable response for root cause analysis")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
