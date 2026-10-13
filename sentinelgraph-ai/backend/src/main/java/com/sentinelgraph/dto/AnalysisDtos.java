package com.sentinelgraph.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public final class AnalysisDtos {
    private AnalysisDtos() {}
    public record AnalyzeRequest(@NotBlank(message="A URL is required") String url) {}
    public record Evidence(String ruleId,String title,String description,String source,String kind,Instant observedAt) {}
    public record AnalysisResponse(String id,String submittedUrl,String normalizedUrl,String hostname,String registrableDomain,
        String claimedBrand,String referenceDomain,String verdict,String threatStatus,Integer score,String analysisMode,
        List<Evidence> evidence,List<String> limitations,Instant createdAt,
        String riskLevel,String safetyAssessment) {}
    public record DashboardStats(long total,long suspicious,long impersonation,long verified,long inconclusive,long openCases,List<AnalysisResponse> recent) {}
    public record NewCaseRequest(@NotBlank String analysisId,@NotBlank String title,String priority) {}
}
