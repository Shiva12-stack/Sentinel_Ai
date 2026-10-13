package com.sentinelgraph.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InternetDomainName;
import com.sentinelgraph.dto.AnalysisDtos.AnalysisResponse;
import com.sentinelgraph.dto.AnalysisDtos.Evidence;
import com.sentinelgraph.model.AnalysisRecord;
import com.sentinelgraph.repository.AnalysisRepository;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.IDN;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Offline-only URL analysis: never connects to the user-submitted URL. */
@Service
public class UrlAnalysisService {
    private static final Map<String,String> FICTIONAL_REFERENCES = Map.of(
        "ShopKart", "shopkart.example", "NovaBank", "novabank.example",
        "CloudCart", "cloudcart.example", "QuickPay", "quickpay.example");
    private final AnalysisRepository repository;
    private final ObjectMapper mapper;
    private final LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();
    public UrlAnalysisService(AnalysisRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}

    public AnalysisResponse analyze(String raw) {
        if(raw==null || raw.isBlank() || raw.length()>2048)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid URL (maximum 2048 characters).");
        String input=raw.trim();
        String complete=input.matches("(?i)^[a-z][a-z0-9+.-]*://.*")?input:"https://"+input;
        URI uri;
        try { uri=URI.create(complete); }
        catch(IllegalArgumentException e){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Malformed URL."); }
        if(uri.getScheme()==null || !(uri.getScheme().equalsIgnoreCase("http")||uri.getScheme().equalsIgnoreCase("https")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only HTTP(S) URLs are supported.");
        if(uri.getRawUserInfo()!=null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"URLs containing embedded credentials are not accepted.");
        if(uri.getRawQuery()!=null && uri.getRawQuery().length()>512)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The query string is too long.");
        String rawHost=uri.getHost();
        if(rawHost==null || rawHost.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"URL must contain a valid hostname.");
        String host;
        try{host=IDN.toASCII(rawHost.replaceAll("\\.$",""),IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hostname is invalid.");}
        if(!host.contains(".") || host.length()>253)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid domain name is required.");
        String registered=registeredDomain(host);
        // Redact URL fragments and query parameters; don't store tokens that might appear in submitted URLs.
        String path=uri.getRawPath()==null||uri.getRawPath().isBlank()?"/":uri.getRawPath();
        String normalized=uri.getScheme().toLowerCase(Locale.ROOT)+"://"+host+(uri.getPort()>0?":"+uri.getPort():"")+path;
        Instant timestamp=Instant.now();
        String claimed=null,reference=null;
        boolean exact=false,lookalike=false;
        String compactHost=registered.substring(0,registered.indexOf('.')).replace("-","").replace("_","");
        for(var entry:FICTIONAL_REFERENCES.entrySet()) {
            String brand=entry.getKey().toLowerCase(Locale.ROOT);
            String whole=host.replace("-","").replace("_","");
            int levenshtein=distance.apply(compactHost,brand);
            if(whole.contains(brand)||levenshtein<=1) {
                claimed=entry.getKey();reference=entry.getValue();
                exact=host.equals(reference)||host.endsWith("."+reference);
                lookalike=!exact;
                break;
            }
        }
        List<Evidence> evidence=new ArrayList<>();
        evidence.add(new Evidence("HOSTNAME_PARSED","Actual hostname identified",
            "The normalized hostname is "+host+"; the registrable domain is "+registered+".","Offline Java URL parser","neutral",timestamp));
        if(exact) evidence.add(new Evidence("FICTIONAL_REFERENCE_MATCH","Fictional reference-domain match",
            "The host matches the seeded "+claimed+" demonstration domain. This does NOT verify real-world brand ownership.","Fictional seed catalog","positive",timestamp));
        else if(lookalike) evidence.add(new Evidence("BRAND_LOOKALIKE","Brand-name resemblance",
            "The domain resembles fictional brand "+claimed+" but differs from reference "+reference+". This alone does not prove fraud.","Offline deterministic rule","warning",timestamp));
        else evidence.add(new Evidence("REFERENCE_NOT_AVAILABLE","No official reference established",
            "No matching brand exists in the small fictional seed catalog. An unknown site is not necessarily malicious.","Fictional seed catalog","neutral",timestamp));
        if(host.matches(".*(login|secure|verify|auth|reward|bonus|update).*"))
            evidence.add(new Evidence("SENSITIVE_HOSTNAME_TERMS","Sign-in or promotional words in hostname",
                "The hostname contains sign-in, verification or promotional terms. Consider the surrounding context.","Offline deterministic rule","warning",timestamp));
        if(host.contains("xn--"))
            evidence.add(new Evidence("PUNYCODE_PRESENT","Encoded internationalized hostname",
                "The hostname contains punycode; evaluate whether visually confusable characters are present.","Offline IDN normalization","neutral",timestamp));
        String verdict=exact?"VERIFIED_REFERENCE_MATCH":lookalike?"SUSPICIOUS":"INCONCLUSIVE";
        List<String> limitations=List.of(
            "The reference catalog contains fictional demonstration brands; these are not independently verified real-world records.",
            "No external threat-intelligence, RDAP, DNS or Certificate Transparency service was contacted.",
            "The target website, redirects, HTML, and logos were not inspected.",
            "A reference match does not guarantee the site is safe, and a lookalike does not prove malicious intent.");
        AnalysisRecord record=new AnalysisRecord();
        record.setId(UUID.randomUUID().toString());
        // Submitted input might contain tokens; store only normalized URL without query/fragment.
        record.setSubmittedUrl(normalized);record.setNormalizedUrl(normalized);record.setHostname(host);
        record.setRegistrableDomain(registered);record.setClaimedBrand(claimed);record.setReferenceDomain(reference);
        record.setVerdict(verdict);record.setThreatStatus("NOT_CHECKED");record.setScore(null);
        record.setAnalysisMode("OFFLINE_RULES_DEMO_REFERENCES");record.setCreatedAt(timestamp);
        try {record.setEvidenceJson(mapper.writeValueAsString(evidence));record.setLimitationsJson(mapper.writeValueAsString(limitations));}
        catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize evidence",e);}
        return toResponse(repository.save(record));
    }
    public AnalysisResponse findById(String id){return repository.findById(id).map(this::toResponse).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Analysis not found."));}
    public List<AnalysisResponse> findAll(){return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();}
    public String registeredDomain(String host) {
        if(host.endsWith(".example")) {String[] parts=host.split("\\.");return parts.length>=2?parts[parts.length-2]+".example":host;}
        try {InternetDomainName name=InternetDomainName.from(host);return name.isUnderPublicSuffix()?name.topPrivateDomain().toString():host;}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid domain structure.");}
    }
    private AnalysisResponse toResponse(AnalysisRecord record){
        try {
            List<Evidence> evidence=mapper.readValue(record.getEvidenceJson(),new TypeReference<List<Evidence>>(){});
            List<String> limitations=mapper.readValue(record.getLimitationsJson(),new TypeReference<List<String>>(){});
            return new AnalysisResponse(record.getId(),record.getSubmittedUrl(),record.getNormalizedUrl(),record.getHostname(),record.getRegistrableDomain(),
                record.getClaimedBrand(),record.getReferenceDomain(),record.getVerdict(),record.getThreatStatus(),record.getScore(),record.getAnalysisMode(),evidence,limitations,record.getCreatedAt());
        }catch(JsonProcessingException e){throw new IllegalStateException("Could not read stored evidence",e);}
    }
}
