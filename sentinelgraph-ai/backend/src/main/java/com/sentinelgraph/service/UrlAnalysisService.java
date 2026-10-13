package com.sentinelgraph.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelgraph.dto.AnalysisDtos.AnalysisResponse;
import com.sentinelgraph.dto.AnalysisDtos.Evidence;
import com.sentinelgraph.model.AnalysisRecord;
import com.sentinelgraph.repository.AnalysisRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Persist analyses while keeping the existing API response and frontend unchanged. */
@Service
public class UrlAnalysisService {
    private final AnalysisRepository repository;
    private final ObjectMapper mapper;
    private final UrlRiskEngine engine;
    public UrlAnalysisService(AnalysisRepository repository,ObjectMapper mapper,UrlRiskEngine engine){
        this.repository=repository;this.mapper=mapper;this.engine=engine;
    }
    public AnalysisResponse analyze(String raw){
        UrlRiskEngine.Assessment result=engine.assess(raw);
        AnalysisRecord record=new AnalysisRecord();
        record.setId(UUID.randomUUID().toString());
        record.setSubmittedUrl(result.url()); record.setNormalizedUrl(result.url());
        record.setHostname(result.hostname());record.setRegistrableDomain(result.registrableDomain());
        record.setClaimedBrand(result.claimedBrand());record.setReferenceDomain(result.referenceDomain());
        record.setVerdict(result.verdict());record.setThreatStatus(result.threatStatus());
        record.setScore(result.score());record.setAnalysisMode(result.analysisMode());
        record.setCreatedAt(Instant.now());
        try{
            record.setEvidenceJson(mapper.writeValueAsString(result.evidence()));
            record.setLimitationsJson(mapper.writeValueAsString(result.limitations()));
        }catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize evidence",e);}
        return toResponse(repository.save(record));
    }
    public AnalysisResponse findById(String id){
        return repository.findById(id).map(this::toResponse)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Analysis not found."));
    }
    public List<AnalysisResponse> findAll(){return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();}
    private AnalysisResponse toResponse(AnalysisRecord record){
        try{
            List<Evidence> evidence=mapper.readValue(record.getEvidenceJson(),new TypeReference<List<Evidence>>(){});
            List<String> limitations=mapper.readValue(record.getLimitationsJson(),new TypeReference<List<String>>(){});
            String verdict=record.getVerdict();
            Integer score=record.getScore();
            String level=score==null?"NOT_SCORED":score>=70?"HIGH":score>=35?"MEDIUM":
                verdict.equals("VERIFIED_REFERENCE_MATCH")?"REFERENCE_MATCH_UNVERIFIED":"LOW";
            String safety=score==null?"NOT_ASSESSED":score>=35?"POTENTIAL_RISK":
                verdict.equals("VERIFIED_REFERENCE_MATCH")?"REFERENCE_MATCH_NOT_SAFETY_PROOF":"INSUFFICIENT_EVIDENCE_TO_DECLARE_SAFE";
            return new AnalysisResponse(record.getId(),record.getSubmittedUrl(),record.getNormalizedUrl(),
                record.getHostname(),record.getRegistrableDomain(),record.getClaimedBrand(),record.getReferenceDomain(),
                verdict,record.getThreatStatus(),score,record.getAnalysisMode(),evidence,limitations,
                record.getCreatedAt(),level,safety);
        }catch(JsonProcessingException e){throw new IllegalStateException("Could not read stored evidence",e);}
    }
    public String registeredDomain(String host){return engine.registeredDomain(host);}
}
