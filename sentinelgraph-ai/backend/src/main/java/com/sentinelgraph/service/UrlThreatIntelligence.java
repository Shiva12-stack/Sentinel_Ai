package com.sentinelgraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelgraph.dto.AnalysisDtos.Evidence;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Optional provider calls: fixed provider endpoints, no user-target URL fetch, no redirects.
 * Both providers are opt-in and keys never appear in persisted evidence or HTTP responses.
 */
@Component
public class UrlThreatIntelligence {
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final String safeBrowsingKey=System.getenv().getOrDefault("SENTINEL_SAFE_BROWSING_KEY","").trim();
    private final String virusTotalKey=System.getenv().getOrDefault("SENTINEL_VIRUSTOTAL_KEY","").trim();
    private final boolean dnsEnabled=Boolean.parseBoolean(System.getenv().getOrDefault("SENTINEL_DNS_ENABLED","false"));
    private final boolean ctEnabled=Boolean.parseBoolean(System.getenv().getOrDefault("SENTINEL_CT_ENABLED","false"));
    private final boolean includePath=Boolean.parseBoolean(System.getenv().getOrDefault("SENTINEL_TI_INCLUDE_PATH","false"));

    public UrlThreatIntelligence(ObjectMapper mapper){this.mapper=mapper;}
    public record Result(List<Evidence> evidence,List<String> limitations,int reputationPoints,boolean checked,String threatStatus) {}
    public boolean configured(){return !safeBrowsingKey.isBlank()||!virusTotalKey.isBlank()||dnsEnabled||ctEnabled;}
    public Map<String,Object> sourceStatus(){return Map.of(
        "safeBrowsing",safeBrowsingKey.isBlank()?"NOT_CONFIGURED":"CONFIGURED",
        "virusTotal",virusTotalKey.isBlank()?"NOT_CONFIGURED":"CONFIGURED",
        "dnsLookup",dnsEnabled?"ENABLED":"DISABLED",
        "certificateTransparency",ctEnabled?"ENABLED":"DISABLED",
        "websiteInspection","DISABLED", "urlPathDisclosureToProviders",includePath?"ENABLED":"DISABLED");}

    public Result lookup(String cleanUrl,String origin,String host,String registrable,Instant time){
        List<Evidence> evidence=new ArrayList<>(); List<String> limitations=new ArrayList<>();
        int risk=0; boolean checked=false; boolean attempted=false;boolean unavailable=false;
        // Default: disclose only the origin, not arbitrary paths or credentials.
        String url=includePath?cleanUrl:origin;
        if(!safeBrowsingKey.isBlank()){
            attempted=true;
            try{
                String endpoint="https://safebrowsing.googleapis.com/v4/threatMatches:find?key="
                    +URLEncoder.encode(safeBrowsingKey,StandardCharsets.UTF_8);
                Map<String,Object> req=Map.of(
                    "client",Map.of("clientId","sentinelgraph-ai","clientVersion","1.0"),
                    "threatInfo",Map.of("threatTypes",List.of("MALWARE","SOCIAL_ENGINEERING","UNWANTED_SOFTWARE","POTENTIALLY_HARMFUL_APPLICATION"),
                        "platformTypes",List.of("ANY_PLATFORM"),"threatEntryTypes",List.of("URL"),
                        "threatEntries",List.of(Map.of("url",url))));
                HttpRequest call=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(3))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(req))).build();
                JsonNode result=json(call);
                checked=true;
                JsonNode matches=result.path("matches");
                if(matches.isArray()&&matches.size()>0){
                    risk=Math.max(risk,95);
                    evidence.add(new Evidence("GOOGLE_SAFE_BROWSING_MATCH","Listed in Google Safe Browsing",
                        "A URL associated with this origin was returned in an unsafe-list match.","Google Safe Browsing","warning",time));
                }else{
                    evidence.add(new Evidence("GOOGLE_SAFE_BROWSING_NO_MATCH","Not listed in checked Google lists",
                        "No match was returned for the checked URL; this is not evidence that it is safe.","Google Safe Browsing","neutral",time));
                }
            }catch(Exception e){unavailable=true;limitations.add("Safe Browsing lookup unavailable or quota-limited; no risk reduction applied.");}
        }
        if(!virusTotalKey.isBlank()){
            attempted=true;
            try{
                String urlId=Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
                HttpRequest call=HttpRequest.newBuilder(URI.create("https://www.virustotal.com/api/v3/urls/"+urlId))
                    .timeout(Duration.ofSeconds(3)).header("x-apikey",virusTotalKey)
                    .header("Accept","application/json").GET().build();
                JsonNode data=json(call);
                checked=true;
                JsonNode counts=data.path("data").path("attributes").path("last_analysis_stats");
                if(!counts.isObject())throw new IllegalStateException("URL report statistics unavailable");
                int malicious=counts.path("malicious").asInt(0);
                int suspicious=counts.path("suspicious").asInt(0);
                if(malicious>0||suspicious>0){
                    risk=Math.max(risk,malicious>0?90:65);
                    evidence.add(new Evidence("VIRUSTOTAL_DETECTIONS","VirusTotal engines reported detections",
                        "Engine counts reported: malicious="+malicious+", suspicious="+suspicious+". This is third-party reputation data, not a confirmed verdict.",
                        "VirusTotal","warning",time));
                }else{
                    evidence.add(new Evidence("VIRUSTOTAL_NO_DETECTIONS","No detections in current URL report",
                        "No malicious or suspicious engines were counted in the retrieved report; coverage may be incomplete or stale.",
                        "VirusTotal","neutral",time));
                }
            }catch(Exception e){unavailable=true;limitations.add("VirusTotal URL report unavailable, unknown or quota-limited; it was not scanned or submitted.");}
        }
        if(dnsEnabled){
            attempted=true;
            try{
                String dnsUrl="https://dns.google/resolve?name="+URLEncoder.encode(host,StandardCharsets.UTF_8)+"&type=A";
                JsonNode result=json(HttpRequest.newBuilder(URI.create(dnsUrl)).timeout(Duration.ofSeconds(3)).GET().build());
                checked=true;
                int responseCode=result.path("Status").asInt(-1);
                boolean hasRecords=result.path("Answer").isArray()&&result.path("Answer").size()>0;
                evidence.add(new Evidence("DNS_QUERY","DNS lookup result",
                    "DNS status="+responseCode+", answer records="+(hasRecords?"present":"absent")+". DNS is not a website safety check.",
                    "Google Public DNS JSON API","neutral",time));
            }catch(Exception e){limitations.add("DNS lookup unavailable; DNS does not determine website safety.");}
        }
        if(ctEnabled){
            attempted=true;
            try{
                String ctUrl="https://api.certspotter.com/v1/issuances?domain="
                    +URLEncoder.encode(registrable,StandardCharsets.UTF_8)+"&include_subdomains=false";
                JsonNode result=json(HttpRequest.newBuilder(URI.create(ctUrl)).timeout(Duration.ofSeconds(3)).GET().build());
                checked=true;
                evidence.add(new Evidence("CERTIFICATE_TRANSPARENCY","Certificate Transparency metadata query",
                    "Certificate Transparency issuance entries returned: "+(result.isArray()?result.size():0)
                        +". This does not test the live website's TLS certificate or trustworthiness.",
                    "Cert Spotter public CT API","neutral",time));
            }catch(Exception e){limitations.add("Certificate Transparency lookup unavailable or rate-limited. Live TLS was not inspected.");}
        }
        String status=risk>0?"LISTED_OR_DETECTED":checked?(unavailable?"PARTIALLY_CHECKED":"CHECKED_NO_LISTING"):
            attempted?"PROVIDERS_UNAVAILABLE":"NOT_CONFIGURED";
        if(!attempted)limitations.add("External reputation, DNS and Certificate Transparency lookups are disabled until configured on Render.");
        if(!includePath && (!safeBrowsingKey.isBlank()||!virusTotalKey.isBlank()))
            limitations.add("Reputation providers checked only the URL origin, not path-specific threats, to minimize data disclosure.");
        return new Result(List.copyOf(evidence),List.copyOf(limitations),risk,checked,status);
    }

    private JsonNode json(HttpRequest request) throws Exception {
        HttpResponse<InputStream> response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream stream=response.body()){
            int status=response.statusCode();
            if(status!=200)throw new IllegalStateException("Provider HTTP "+status);
            byte[] bytes=stream.readNBytes(65537);
            if(bytes.length>65536)throw new IllegalStateException("Provider response too large");
            return mapper.readTree(bytes);
        }
    }
}
