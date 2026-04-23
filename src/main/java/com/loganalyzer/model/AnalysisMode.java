package com.loganalyzer.model;

/**
 * Day 2: Different analysis modes map to different temperature settings.
 * - EXPLAIN: temperature=0.7 (creative, human-readable explanation)
 * - DETECT / SUMMARIZE / ROOT_CAUSE: temperature=0.0 (deterministic, precise)
 */
public enum AnalysisMode {
    EXPLAIN,
    DETECT_ERRORS,
    SUMMARIZE,
    ROOT_CAUSE
}
