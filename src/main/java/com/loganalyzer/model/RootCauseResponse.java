package com.loganalyzer.model;

import java.util.List;

public record RootCauseResponse(
        List<String> observedSymptoms,
        String mostLikelyRootCause,
        List<String> contributingFactors,
        List<String> recommendedFixes
) {}
