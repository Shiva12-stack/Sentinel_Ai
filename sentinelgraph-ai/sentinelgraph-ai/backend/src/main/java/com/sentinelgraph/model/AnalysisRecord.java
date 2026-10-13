package com.sentinelgraph.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {
    @Id
    private String id;
    @Column(nullable=false,length=2048)
    private String submittedUrl;
    @Column(nullable=false,length=2048)
    private String normalizedUrl;
    private String hostname;
    private String registrableDomain;
    private String claimedBrand;
    private String referenceDomain;
    @Column(nullable=false)
    private String verdict;
    private String threatStatus;
    private Integer score;
    private String analysisMode;
    @Column(columnDefinition="text")
    private String evidenceJson;
    @Column(columnDefinition="text")
    private String limitationsJson;
    private Instant createdAt;

    public AnalysisRecord() {}
    public String getId(){return id;} public void setId(String x){id=x;}
    public String getSubmittedUrl(){return submittedUrl;} public void setSubmittedUrl(String x){submittedUrl=x;}
    public String getNormalizedUrl(){return normalizedUrl;} public void setNormalizedUrl(String x){normalizedUrl=x;}
    public String getHostname(){return hostname;} public void setHostname(String x){hostname=x;}
    public String getRegistrableDomain(){return registrableDomain;} public void setRegistrableDomain(String x){registrableDomain=x;}
    public String getClaimedBrand(){return claimedBrand;} public void setClaimedBrand(String x){claimedBrand=x;}
    public String getReferenceDomain(){return referenceDomain;} public void setReferenceDomain(String x){referenceDomain=x;}
    public String getVerdict(){return verdict;} public void setVerdict(String x){verdict=x;}
    public String getThreatStatus(){return threatStatus;} public void setThreatStatus(String x){threatStatus=x;}
    public Integer getScore(){return score;} public void setScore(Integer x){score=x;}
    public String getAnalysisMode(){return analysisMode;} public void setAnalysisMode(String x){analysisMode=x;}
    public String getEvidenceJson(){return evidenceJson;} public void setEvidenceJson(String x){evidenceJson=x;}
    public String getLimitationsJson(){return limitationsJson;} public void setLimitationsJson(String x){limitationsJson=x;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant x){createdAt=x;}
}
