package com.loganalyzer.model;

public record LogError(
        int lineNumber,
        String timestamp,
        String level,
        String message
) {}
