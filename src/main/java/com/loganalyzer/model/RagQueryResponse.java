package com.loganalyzer.model;

import java.util.List;

public record RagQueryResponse(
        String answer,
        List<String> relevantChunks
) {}
