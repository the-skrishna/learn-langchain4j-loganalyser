package com.loganalyzer.model;

import java.util.List;

public record ErrorDetectionResponse(
        int totalErrors,
        List<LogError> errors
) {
}
