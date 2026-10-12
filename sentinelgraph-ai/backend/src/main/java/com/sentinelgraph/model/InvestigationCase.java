package com.sentinelgraph.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="investigation_cases")
public class InvestigationCase {
    @Id private String id;
    @Column(nullable=false) private String title;
    private String priority;
    private String status;
    private String analysisId;
    @Column(columnDefinition="text") private String notes;
    private Instant createdAt;
    public InvestigationCase() {}
    public String getId(){return id;} public void setId(String x){id=x;}
    public String getTitle(){return title;} public void setTitle(String x){title=x;}
    public String getPriority(){return priority;} public void setPriority(String x){priority=x;}
    public String getStatus(){return status;} public void setStatus(String x){status=x;}
    public String getAnalysisId(){return analysisId;} public void setAnalysisId(String x){analysisId=x;}
    public String getNotes(){return notes;} public void setNotes(String x){notes=x;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant x){createdAt=x;}
}
