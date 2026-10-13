package com.sentinelgraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InternetDomainName;
import com.sentinelgraph.dto.AnalysisDtos.Evidence;
import com.sentinelgraph.observatory.RecordDocument;
import com.sentinelgraph.observatory.RecordStore;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.IDN;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/** Shared URL-only detection for the web analyzer and the Social & Apps website form.
 * It never opens an untrusted destination, follows links, or claims to verify ownership.
 */
@Service
public class UrlRiskEngine {
    private static final Map<String,List<String>> DEMO = Map.of(
        "ShopKart", List.of("shopkart.example"), "NovaBank", List.of("novabank.example"),
        "CloudCart", List.of("cloudcart.example"), "QuickPay", List.of("quickpay.example"));
    // Publicly known names are *comparison hints*, not an ownership-verification service.
    private static final Map<String,List<String>> COMMON_REFERENCES = Map.of(
        "Amazon", List.of("amazon.com", "amazon.in", "amazon.co.uk", "amazon.de", "amazon.ca", "amazon.co.jp", "amazon.fr", "amazon.it", "amazon.es", "amazon.com.au", "amazon.ae"),
        "Google", List.of("google.com"), "PayPal", List.of("paypal.com"),
        "Microsoft", List.of("microsoft.com"), "Flipkart", List.of("flipkart.com"),
        "Instagram", List.of("instagram.com"), "Netflix", List.of("netflix.com"));
    private static final Pattern SENSITIVE_HOST = Pattern.compile("(?i)(login|log-in|signin|sign-in|verify|secure|password|reset|otp|claim|reward|bonus|auth)");
    private static final Pattern SENSITIVE_PATH = Pattern.compile("(?i)(verify-password|send-otp|password-reset|claim-reward|reset-password)");
    private static final Pattern IPV4 = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    private final ObjectMapper mapper;
    private final RecordStore store;
    private final UrlThreatIntelligence intelligence;
    private final LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();

    public UrlRiskEngine(RecordStore store, ObjectMapper mapper, UrlThreatIntelligence intelligence) {
        this.store=store; this.mapper=mapper; this.intelligence=intelligence;
    }
    public record Assessment(String url,String hostname,String registrableDomain,String claimedBrand,
        String referenceDomain,String verdict,String threatStatus,int score,String analysisMode,
        List<Evidence> evidence,List<String> limitations,String riskLevel,String safetyAssessment) {}
    private record Reference(String brand,List<String> domains,String origin) {}

    public Assessment assess(String raw) {
        if(raw==null||raw.isBlank()||raw.length()>2048)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter an HTTP(S) URL (up to 2048 characters).");
        String input=raw.trim();
        String complete=input.matches("(?i)^[a-z][a-z0-9+.-]*://.*")?input:"https://"+input;
        URI uri;
        try{uri=URI.create(complete);}catch(IllegalArgumentException e){throw bad("Malformed URL.");}
        if(uri.getScheme()==null||!(uri.getScheme().equalsIgnoreCase("http")||uri.getScheme().equalsIgnoreCase("https")))throw bad("Only HTTP(S) URLs are supported.");
        if(uri.getRawUserInfo()!=null)throw bad("URLs with embedded credentials are not accepted.");
        if(uri.getRawQuery()!=null&&uri.getRawQuery().length()>512)throw bad("URL query is too long.");
        if(uri.getHost()==null||uri.getHost().isBlank())throw bad("URL must contain a valid hostname.");
        if(uri.getPort()>65535)throw bad("Invalid port.");
        String host;
        try{host=IDN.toASCII(uri.getHost().replaceAll("\\.$",""),IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);}
        catch(IllegalArgumentException e){throw bad("Invalid hostname.");}
        if(host.length()>253||!host.contains(".")||IPV4.matcher(host).matches()||host.endsWith(".local")||host.endsWith(".internal"))throw bad("Enter a public DNS hostname.");
        String registered=registeredDomain(host);
        if(registered.equals(host)&&!host.endsWith(".example")&&!InternetDomainName.from(host).isUnderPublicSuffix())throw bad("Hostname has no recognized registrable domain.");
        String path=uri.getRawPath()==null||uri.getRawPath().isBlank()?"/":uri.getRawPath();
        // Never store or transmit submitted query parameters / fragments.
        String clean=uri.getScheme().toLowerCase(Locale.ROOT)+"://"+host+(uri.getPort()>=0?":"+uri.getPort():"")+path;
        String safeOrigin=uri.getScheme().toLowerCase(Locale.ROOT)+"://"+host+"/";
        Instant now=Instant.now();
        List<Evidence> evidence=new ArrayList<>();
        evidence.add(new Evidence("HOSTNAME_PARSED","Hostname and actual domain identified",
            "The registrable domain is "+registered+"; the complete hostname is "+host+".","Public Suffix List / URL parser","neutral",now));

        Reference best=null; boolean exact=false; int rank=-1;
        String labels[]=host.split("\\.");
        String hostnameCompact=compact(host);
        String domainLabel=registered.split("\\.")[0];
        String lookalike=visual(compact(domainLabel));
        for(Reference ref:references()){
            String key=compact(ref.brand());
            if(key.length()<4)continue;
            boolean official=ref.domains().stream().anyMatch(d->host.equals(d)||host.endsWith("."+d));
            boolean similar=hostnameCompact.contains(key)||visual(hostnameCompact).contains(key)
                ||(Math.abs(lookalike.length()-key.length())<=1 && distance.apply(lookalike,key)<=1);
            if(!official&&!similar)continue;
            int confidence=(official?100000:0)+key.length()*10+(ref.origin().equals("USER")?3:0);
            if(confidence>rank){rank=confidence;best=ref;exact=official;}
        }
        int score=0;
        if(best!=null){
            String brand=best.brand();
            if(exact){
                evidence.add(new Evidence("REGISTERED_REFERENCE_MATCH","Matches a supplied reference hostname",
                    "This hostname is within a recorded reference for "+brand+"; this does not verify current ownership or content.",best.origin()+" reference","positive",now));
            }else if(best.domains().isEmpty()){
                evidence.add(new Evidence("BRAND_NAME_ONLY","Brand name resembles a saved brand without a website reference",
                    "No official Website asset was supplied for this brand. Similarity alone is not a website mismatch.",
                    best.origin()+" reference","neutral",now));
            }else{
                add(evidence,"BRAND_RESEMBLANCE",40,"Possible brand-name impersonation",
                    "The hostname resembles "+brand+" but is not one of its recorded reference domains.",now);
                score+=40;
                if(!best.domains().isEmpty()){
                    add(evidence,"OFFICIAL_DOMAIN_MISMATCH",20,"Hostname differs from the brand reference",
                        "The registrable domain is "+registered+", compared with the reference "+best.domains().get(0)+".",now);
                    score+=20;
                }
                boolean changed=false;
                String key=compact(brand);
                for(String label:labels){
                    String normalized=compact(label);
                    if(!normalized.equals(key) && visual(normalized).contains(key)
                        && (normalized.matches(".*\\d.*")||!normalized.contains(key)))changed=true;
                }
                if(changed){
                    add(evidence,"BRAND_DIGIT_VARIATION",30,"Altered brand spelling or added digits",
                        "The hostname includes a digit-modified or extended form resembling a brand name.",now);
                    score+=30;
                }
                boolean embedded=false;
                for(int i=0;i<labels.length-2;i++){
                    if(compact(labels[i]).contains(key)||visual(compact(labels[i])).contains(key))embedded=true;
                }
                if(embedded){
                    add(evidence,"BRAND_IN_UNRELATED_SUBDOMAIN",15,"Brand-like name used inside another domain",
                        "A brand-like label appears before the actual registrable domain "+registered+".",now);
                    score+=15;
                }
            }
        }else{
            evidence.add(new Evidence("BRAND_NOT_IDENTIFIED","No unambiguous brand reference matched",
                "Unknown brand references do not establish safety. Structural checks still apply.","Saved and built-in references","neutral",now));
        }
        if(!exact){
            boolean sensitive=SENSITIVE_HOST.matcher(host).find();
            if(sensitive){
                // Standalone common login subdomain is weak; login + brand-like identity is stronger.
                int points=best!=null?25:15;
                add(evidence,"SIGN_IN_HOSTNAME",points,"Login, verification or promotional hostname wording",
                    "Sensitive words appear in the hostname. The site was not opened.",now);
                score+=points;
            }
            boolean multiLabelDecoy=false;
            for(int i=0;i<labels.length-2;i++){
                if((labels[i].equals("com")||labels[i].equals("net")||labels[i].equals("org")))multiLabelDecoy=true;
            }
            if(multiLabelDecoy){add(evidence,"MISLEADING_SUBDOMAIN",35,"Domain extension embedded in hostname",
                "A domain-extension label is embedded before the true registrable domain.",now);score+=35;}
            if(uri.getScheme().equalsIgnoreCase("http")){
                add(evidence,"HTTP_URL",10,"URL uses HTTP", "HTTPS was not specified. Transport security was not tested.",now);score+=10;
            }
            if(host.contains("xn--")){
                add(evidence,"PUNYCODE_HOST",15,"IDN-encoded hostname", "An encoded name can be legitimate; investigate context.",now);score+=15;
            }
            if(SENSITIVE_PATH.matcher(path).find()){
                add(evidence,"SENSITIVE_PATH",15,"Credential or reward path wording", "The path contains a sensitive phrase.",now);score+=15;
            }
        }
        score=Math.min(100,score);
        // Reputation requests go only to hard-coded provider endpoints. Never to the submitted website.
        UrlThreatIntelligence.Result ti=intelligence.lookup(clean,safeOrigin,host,registered,now);
        evidence.addAll(ti.evidence());
        if(!exact && ti.reputationPoints()>0)score=Math.max(score,ti.reputationPoints());
        // A provider finding overrides a user-supplied reference matching the hostname.
        if(exact&&ti.reputationPoints()>=70)score=ti.reputationPoints();
        String verdict=score>=70 && (best!=null && !exact)?"LIKELY_IMPERSONATION":
            score>=35?"SUSPICIOUS":exact?"VERIFIED_REFERENCE_MATCH":"INCONCLUSIVE";
        String level=score>=70?"HIGH":score>=35?"MEDIUM":exact?"REFERENCE_MATCH_UNVERIFIED":"LOW";
        String safety=score>=35?"POTENTIAL_RISK":exact?"REFERENCE_MATCH_NOT_SAFETY_PROOF":"INSUFFICIENT_EVIDENCE_TO_DECLARE_SAFE";
        evidence.add(new Evidence("EXPLAINABLE_RISK_SCORE","Heuristic risk score: "+score+"/100",
            "This score counts warning signals, not the probability of fraud; 0 does not prove safety.","URL rules + enabled reputation providers",score>=35?"warning":"neutral",now));
        List<String> limitations=new ArrayList<>(List.of(
            "The target website was not opened. HTML, redirects, passwords, images and live TLS certificates were not inspected.",
            "Built-in and user-entered brand references do not independently verify domain ownership.",
            "Threat provider coverage is partial; no listing or provider timeout is not proof of safety.",
            "URL query parameters and fragments are never stored or sent to external providers.",
            "A score is a heuristic indicator, not an estimated fraud probability."));
        limitations.addAll(ti.limitations());
        String mode=ti.checked()?"URL_HEURISTICS_AND_CONFIGURED_INTELLIGENCE":"URL_HEURISTICS_REFERENCE_COMPARISON";
        return new Assessment(clean,host,registered,best==null?null:best.brand(),
            best==null||best.domains().isEmpty()?null:best.domains().stream()
                .filter(d->host.equals(d)||host.endsWith("."+d)).findFirst().orElse(best.domains().get(0)),verdict,
            ti.threatStatus(),score,mode,List.copyOf(evidence),List.copyOf(limitations),level,safety);
    }
    private ResponseStatusException bad(String msg){return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);}
    private void add(List<Evidence> evidence,String id,int points,String title,String detail,Instant now){
        evidence.add(new Evidence(id,title,detail+" (rule points: "+points+")","URL structure heuristic","warning",now));
    }
    private String compact(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private String visual(String text){return text.replace('0','o').replace('1','l').replace('3','e').replace('5','s');}
    public String registeredDomain(String host){
        if(host.endsWith(".example")){String[] parts=host.split("\\.");return parts.length>=2?parts[parts.length-2]+".example":host;}
        try{InternetDomainName domain=InternetDomainName.from(host);return domain.isUnderPublicSuffix()?domain.topPrivateDomain().toString():host;}
        catch(IllegalArgumentException e){throw bad("Invalid domain structure.");}
    }
    private List<Reference> references(){
        List<Reference> output=new ArrayList<>();
        DEMO.forEach((k,v)->output.add(new Reference(k,v,"SYNTHETIC")));
        COMMON_REFERENCES.forEach((k,v)->output.add(new Reference(k,v,"PUBLIC_COMPARISON_HINT")));
        try{
            Map<String,String> brands=new HashMap<>();
            for(RecordDocument entry:store.findByKind("brand")){
                JsonNode obj=mapper.readTree(entry.payload);
                if(obj.hasNonNull("name"))brands.put(obj.path("id").asText(),obj.path("name").asText());
            }
            Map<String,List<String>> domains=new LinkedHashMap<>();
            for(RecordDocument entry:store.findByKind("asset")){
                JsonNode obj=mapper.readTree(entry.payload);
                String platform=obj.path("platform").asText("");
                if(!(platform.equalsIgnoreCase("website")||platform.equalsIgnoreCase("website / other")))continue;
                String brandId=obj.path("brandId").asText();
                if(!brands.containsKey(brandId))continue;
                try{
                    URI src=URI.create(obj.path("sourceUrl").asText());
                    if(src.getHost()==null)continue;
                    String domain=IDN.toASCII(src.getHost()).toLowerCase(Locale.ROOT);
                    domains.computeIfAbsent(brandId,k->new ArrayList<>()).add(domain);
                }catch(IllegalArgumentException ignored){}
            }
            for(var brand:brands.entrySet())output.add(new Reference(brand.getValue(),domains.getOrDefault(brand.getKey(),List.of()),"USER"));
        }catch(Exception ignored){ // Reference failures should not stop basic URL analyses.
        }
        return output;
    }
}
