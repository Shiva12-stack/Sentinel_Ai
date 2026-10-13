package com.sentinelgraph.controller;

import com.sentinelgraph.dto.AnalysisDtos.AnalysisResponse;
import com.sentinelgraph.dto.AnalysisDtos.AnalyzeRequest;
import com.sentinelgraph.dto.AnalysisDtos.DashboardStats;
import com.sentinelgraph.dto.AnalysisDtos.NewCaseRequest;
import com.sentinelgraph.model.InvestigationCase;
import com.sentinelgraph.repository.AnalysisRepository;
import com.sentinelgraph.repository.CaseRepository;
import com.sentinelgraph.service.UrlAnalysisService;
import com.sentinelgraph.service.UrlThreatIntelligence;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private final UrlAnalysisService analysis;
    private final AnalysisRepository analyses;
    private final CaseRepository cases;
    private final UrlThreatIntelligence intelligence;
    public ApiController(UrlAnalysisService analysis,AnalysisRepository analyses,CaseRepository cases,UrlThreatIntelligence intelligence){
        this.analysis=analysis;this.analyses=analyses;this.cases=cases;this.intelligence=intelligence;
    }
    @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","UP","mode","URL_HEURISTICS_OPTIONAL_REPUTATION","realWebsiteInspection",false);}
    @PostMapping("/analyze/url") public AnalysisResponse analyze(@Valid @RequestBody AnalyzeRequest request){return analysis.analyze(request.url());}
    @GetMapping("/analyses/{id}") public AnalysisResponse one(@PathVariable String id){return analysis.findById(id);}
    @GetMapping("/findings") public List<AnalysisResponse> findings(){return analysis.findAll();}
    @GetMapping("/dashboard/stats") public DashboardStats stats(){
        List<AnalysisResponse> recent=analysis.findAll();
        return new DashboardStats(analyses.count(),analyses.countByVerdict("SUSPICIOUS"),analyses.countByVerdict("LIKELY_IMPERSONATION"),
            analyses.countByVerdict("VERIFIED_REFERENCE_MATCH"),analyses.countByVerdict("INCONCLUSIVE"),
            cases.countByStatusNotIn(List.of("RESOLVED","FALSE_POSITIVE")),recent.stream().limit(6).toList());
    }
    @GetMapping("/cases") public List<InvestigationCase> cases(){return cases.findAllByOrderByCreatedAtDesc();}
    @PostMapping("/cases") @ResponseStatus(HttpStatus.CREATED)
    public InvestigationCase createCase(@Valid @RequestBody NewCaseRequest request){
        if(!analyses.existsById(request.analysisId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Analysis does not exist.");
        if(request.title().length()>180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Title too long.");
        InvestigationCase item=new InvestigationCase();
        item.setId("CASE-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());
        item.setTitle(request.title());item.setAnalysisId(request.analysisId());
        item.setPriority(request.priority()!=null && List.of("LOW","MEDIUM","HIGH").contains(request.priority())?request.priority():"MEDIUM");
        item.setStatus("NEW");item.setNotes("Created from a finding. Analyst review is required.");item.setCreatedAt(Instant.now());
        return cases.save(item);
    }
    @PostMapping("/cases/{id}/status") public InvestigationCase updateCase(@PathVariable String id,@RequestBody Map<String,String> body){
        String status=body.get("status");
        if(!List.of("NEW","TRIAGED","INVESTIGATING","ESCALATED","RESOLVED","FALSE_POSITIVE").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid status");
        InvestigationCase item=cases.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found"));
        item.setStatus(status);return cases.save(item);
    }
    @GetMapping("/status/sources") public Map<String,Object> sources(){return intelligence.sourceStatus();}
    @GetMapping("/discovery/candidates") public Map<String,Object> discoveries(){return Map.of("candidates",List.of(),"status","NO_LIVE_FEEDS","message","Discovery feeds are not connected in this starter.");}
}
