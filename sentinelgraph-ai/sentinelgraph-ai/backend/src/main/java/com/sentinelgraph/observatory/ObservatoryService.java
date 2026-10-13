package com.sentinelgraph.observatory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@Service @Transactional
public class ObservatoryService {
 private final RecordStore store; private final ObjectMapper json;
 public ObservatoryService(RecordStore store,ObjectMapper json){this.store=store;this.json=json;}
 private String now(){return Instant.now().toString();}
 private String id(String prefix){return prefix+"-"+UUID.randomUUID();}
 private Map<String,Object> unpack(RecordDocument r){try{return json.readValue(r.payload,new TypeReference<LinkedHashMap<String,Object>>(){});}catch(Exception e){throw new IllegalStateException(e);}}
 private Map<String,Object> put(String key,String kind,boolean seed,Map<String,Object> data){
  Map<String,Object> value=new LinkedHashMap<>(data);value.put("id",key);
  try{RecordDocument r=store.findById(key).orElseGet(()->new RecordDocument(key,kind,seed,""));r.payload=json.writeValueAsString(value);store.save(r);return value;}catch(Exception e){throw new IllegalStateException(e);}
 }
 private boolean isDemoSeed(String key){return store.findById(key).map(r->r.demoSeed).orElse(false);}
 public List<Map<String,Object>> list(String kind){return store.findByKind(kind).stream().map(this::unpack).sorted(Comparator.comparing(x->String.valueOf(x.getOrDefault("capturedAt",x.getOrDefault("createdAt",x.get("id")))))).toList();}
 public Map<String,Object> get(String key,String kind){RecordDocument r=store.findById(key).orElseThrow(()->bad("Record not found",HttpStatus.NOT_FOUND));if(!r.kind.equals(kind))throw bad("Record type mismatch",HttpStatus.NOT_FOUND);return unpack(r);}
 private ResponseStatusException bad(String msg,HttpStatus status){return new ResponseStatusException(status,msg);}
 private String text(Map<String,Object> m,String field){Object v=m.get(field);return v==null?"":v.toString().trim();}
 private boolean websitePlatform(String platform){return "Website".equalsIgnoreCase(platform)||"Website / other".equalsIgnoreCase(platform);}
 private boolean samePlatform(String left,String right){return left.equalsIgnoreCase(right)||(websitePlatform(left)&&websitePlatform(right));}
 private String required(Map<String,Object> m,String field){String v=text(m,field);if(v.isBlank()||v.length()>250)throw bad(field+" is required (maximum 250 characters)",HttpStatus.BAD_REQUEST);return v;}
 private void url(String s){try{URI u=URI.create(s);if(!List.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}catch(Exception e){throw bad("Use an HTTP(S) source URL without credentials",HttpStatus.BAD_REQUEST);}}
 public Map<String,Object> brand(Map<String,Object> input){String name=required(input,"name");List<Map<String,Object>> existing=list("brand").stream().filter(b->name.equalsIgnoreCase(text(b,"name"))).toList();if(existing.size()==1)return existing.get(0);if(existing.size()>1)throw bad("Multiple registered brands share this name; contact the administrator",HttpStatus.CONFLICT);Map<String,Object> b=new LinkedHashMap<>();b.put("name",name);b.put("createdAt",now());b.put("provenance","USER");return put(id("BRAND"),"brand",false,b);}
 public Map<String,Object> asset(Map<String,Object> input){String brandId=text(input,"brandId");
  if(brandId.isBlank()){
   String lookup=required(input,"brandName");
   List<Map<String,Object>> matching=list("brand").stream().filter(b->lookup.equalsIgnoreCase(text(b,"name"))).toList();
   if(matching.size()!=1)throw bad("Enter the exact name of one registered brand",HttpStatus.BAD_REQUEST);
   brandId=text(matching.get(0),"id");
  }
  get(brandId,"brand");String source=required(input,"sourceUrl");url(source);String platform=required(input,"platform");String identifier=required(input,"identifier");
  final String finalBrandId=brandId;
  if(list("asset").stream().anyMatch(a->finalBrandId.equals(a.get("brandId"))&&platform.equals(a.get("platform"))&&identifier.equals(a.get("identifier"))))throw bad("Official asset already exists",HttpStatus.CONFLICT);
  Map<String,Object> a=new LinkedHashMap<>();for(String f:List.of("name","username","logo","description","publisher"))a.put(f,text(input,f));a.putAll(Map.of("brandId",brandId,"platform",platform,"identifier",identifier,"sourceUrl",source,"createdAt",now(),"provenance","USER"));return put(id("ASSET"),"asset",false,a);
 }
 public Map<String,Object> test(Map<String,Object> input){String brand=required(input,"brandName"),candidate=required(input,"candidateName");Map<String,Object> result=new LinkedHashMap<>(Similarity.compare(brand,candidate));
  boolean exact=list("asset").stream().anyMatch(a->brand.equalsIgnoreCase(String.valueOf(get(String.valueOf(a.get("brandId")),"brand").get("name")))&&samePlatform(text(input,"platform"),text(a,"platform"))&&text(input,"identifier").equals(a.get("identifier"))&&!text(input,"identifier").isEmpty());
  result.put("mode","MANUAL_TEST — not a monitoring result");result.put("officialExcluded",exact);result.put("safeguards",List.of("Official exclusion requires matching platform and stable identifier","Short names (<4 normalized characters) require extra evidence","Name similarity is a candidate signal, not proof of maliciousness","No record or finding is created by a manual test"));return result;
 }
 private Map<String,Object> risk(Map<String,Object> c){Map<String,Object> brand=get(text(c,"brandId"),"brand");Map<String,Object> sim=Similarity.compare(text(brand,"name"),text(c,"name"));boolean excluded=list("asset").stream().anyMatch(a->text(c,"brandId").equals(a.get("brandId"))&&samePlatform(text(c,"platform"),text(a,"platform"))&&(text(c,"identifier").equals(a.get("identifier"))||text(c,"sourceUrl").replaceAll("/+$","").equalsIgnoreCase(text(a,"sourceUrl").replaceAll("/+$",""))));
  List<Map<String,Object>> signals=new ArrayList<>();double similarity=((Number)sim.get("similarity")).doubleValue();boolean added=Boolean.TRUE.equals(sim.get("addedWords"));int score=0;
  if(Similarity.normalize(text(brand,"name")).length()>=4&&(similarity>=70||added)){int points=(int)Math.round(40*(added?Math.max(similarity,80):similarity)/100);score+=points;signals.add(Map.of("signal","Name resemblance","points",points,"detail",sim));}
  List<Map<String,Object>> assets=list("asset").stream().filter(a->text(c,"brandId").equals(a.get("brandId"))).toList();
  String logo=text(c,"logo");if(!logo.isEmpty()&&assets.stream().anyMatch(a->logo.equals(a.get("logo")))){score+=25;signals.add(Map.of("signal","Exact supplied logo URL match","points",25,"detail","URL equality; image-content similarity was not performed"));}
  if(text(c,"description").toLowerCase(Locale.ROOT).matches(".*(verify password|send otp|claim reward|urgent payment).*")){score+=25;signals.add(Map.of("signal","Explicit credential/reward/payment language","points",25,"detail",text(c,"description")));}
  String publisher=text(c,"publisher");if(!publisher.isEmpty()&&assets.stream().anyMatch(a->samePlatform(text(c,"platform"),text(a,"platform"))&&!text(a,"publisher").isEmpty()&&!publisher.equalsIgnoreCase(text(a,"publisher")))){score+=10;signals.add(Map.of("signal","Publisher differs from supplied official reference","points",10,"detail",publisher));}
  String profileText=(text(c,"name")+" "+text(c,"username")+" "+text(c,"sourceUrl")).toLowerCase(Locale.ROOT);
  if(!excluded&&profileText.matches(".*(free[-_ ]?coupon|claim[-_ ]?(reward|prize)|free[-_ ]?gift|verify[-_ ]?account|secure[-_ ]?login).*")){
   score+=20;signals.add(Map.of("signal","Promotional or verification wording in supplied identity/URL","points",20,"detail","Metadata indicator only; not a live verification"));
  }
  score=Math.min(100,score);
  // An official reference exclusion overrides the warning points as well as the total:
  // never display positive per-signal points next to a zero overall score.
  if(excluded){score=0;signals.clear();}
  String verdict=excluded?"SUPPLIED_OFFICIAL_ASSET_MATCH":score>=70?"LIKELY_IMPERSONATION":score>=35?"SUSPICIOUS":"INCONCLUSIVE";
  String level=excluded?"REFERENCE_MATCH_UNVERIFIED":score>=70?"HIGH":score>=35?"MEDIUM":"LOW";
  String safety=excluded?"REFERENCE_MATCH_NOT_SAFETY_PROOF":score>=35?"POTENTIAL_RISK":"INSUFFICIENT_EVIDENCE_TO_DECLARE_SAFE";
  Map<String,Object> assessment=new LinkedHashMap<>();
  assessment.put("score",score);
  assessment.put("verdict",verdict);
  assessment.put("riskLevel",level);
  assessment.put("safetyAssessment",safety);
  assessment.put("officialExcluded",excluded);
  assessment.put("signals",signals);
  assessment.put("formula","name ≤40 + exact supplied logo URL 25 + scam text 25 + publisher mismatch 10 + suspicious URL/name wording 20; capped at 100; official reference match => 0");
  assessment.put("missing",List.of("Image-content logo analysis","Account ownership verification","Live source verification"));
  return assessment;
 }
 private String decision(String candidateId){List<Map<String,Object>> d=list("decision").stream().filter(x->candidateId.equals(x.get("candidateId"))).sorted(Comparator.comparing(x->text(x,"createdAt"))).toList();return d.isEmpty()?"UNREVIEWED":text(d.get(d.size()-1),"status");}
 public Map<String,Object> feedback(String candidateId,Map<String,Object> input){get(candidateId,"candidate");String status=required(input,"status");if(!List.of("CONFIRMED","DISMISSED","ALLOWLISTED","UNREVIEWED").contains(status))throw bad("Invalid decision",HttpStatus.BAD_REQUEST);return put(id("DECISION"),"decision",false,Map.of("candidateId",candidateId,"status",status,"note",required(input,"note"),"actor","LOCAL_ANALYST","createdAt",now()));}
 public Map<String,Object> scan(Map<String,Object> input){String brandId=required(input,"brandId"),platform=required(input,"platform");get(brandId,"brand");if(!(input.get("candidates") instanceof List<?> entries)||entries.size()>200)throw bad("Provide up to 200 candidate observations",HttpStatus.BAD_REQUEST);
  String provenance=text(input,"provenance");if(!List.of("MANUAL_IMPORT","SYNTHETIC").contains(provenance))throw bad("Use MANUAL_IMPORT or SYNTHETIC; live connectors are not configured",HttpStatus.BAD_REQUEST);
  String scanId=id("SCAN");List<String> seen=new ArrayList<>();List<Map<String,Object>> changes=new ArrayList<>();Set<String> identifiers=new HashSet<>();
  // Validate the entire batch before mutating it.
  for(Object item:entries){if(!(item instanceof Map<?,?>))throw bad("Invalid candidate",HttpStatus.BAD_REQUEST);@SuppressWarnings("unchecked")Map<String,Object> c=(Map<String,Object>)item;String identifier=required(c,"identifier");required(c,"name");url(required(c,"sourceUrl"));if(!identifiers.add(identifier))throw bad("Duplicate identifier in scan",HttpStatus.BAD_REQUEST);for(String f:List.of("username","publisher","logo","description","contact","outboundUrl"))if(text(c,f).length()>4000)throw bad("Metadata too long",HttpStatus.BAD_REQUEST);}
  for(Object item:entries){@SuppressWarnings("unchecked")Map<String,Object> raw=(Map<String,Object>)item;String identifier=text(raw,"identifier");Map<String,Object> previous=list("candidate").stream().filter(c->brandId.equals(c.get("brandId"))&&platform.equals(c.get("platform"))&&identifier.equals(c.get("identifier"))&&provenance.equals(c.get("provenance"))&&!isDemoSeed(text(c,"id"))).findFirst().orElse(null);
   String candidateId=previous==null?id("CAND"):text(previous,"id");Map<String,Object> c=new LinkedHashMap<>();for(String f:List.of("name","username","logo","description","publisher","contact","outboundUrl","sourceUrl"))c.put(f,text(raw,f));c.putAll(Map.of("brandId",brandId,"platform",platform,"identifier",identifier,"provenance",provenance,"capturedAt",now(),"scanId",scanId,"lifecycle","ACTIVE"));
   List<Map<String,Object>> diff=new ArrayList<>();if(previous!=null)for(String f:List.of("name","username","logo","description","publisher","contact","outboundUrl","sourceUrl","lifecycle"))if(!Objects.equals(previous.get(f),c.get(f)))diff.add(Map.of("field",f,"before",String.valueOf(previous.get(f)),"after",String.valueOf(c.get(f))));
   Map<String,Object> calculation=risk(c);c.put("risk",calculation);c.put("analystStatus",decision(candidateId));put(candidateId,"candidate",false,c);seen.add(candidateId);
   String state=previous==null?"NEW":diff.isEmpty()?"UNCHANGED":"CHANGED";Map<String,Object> obs=new LinkedHashMap<>(c);obs.put("candidateId",candidateId);obs.put("change",state);obs.put("differences",diff);obs.put("screenshotStatus","UNAVAILABLE — no capture performed");obs.put("matchingSignals",calculation.get("signals"));put(id("EVIDENCE"),"evidence",false,obs);changes.add(Map.of("candidateId",candidateId,"change",state,"differences",diff,"name",text(c,"name"),"sourceUrl",text(c,"sourceUrl"),"platform",platform,"risk",calculation));
  }
  // Absence never resolves findings unless the analyst explicitly marks this source batch complete.
  if(Boolean.TRUE.equals(input.get("completeSourceSnapshot")))for(Map<String,Object> c:list("candidate"))if(brandId.equals(c.get("brandId"))&&platform.equals(c.get("platform"))&&provenance.equals(c.get("provenance"))&&!isDemoSeed(text(c,"id"))&&!seen.contains(c.get("id"))&&!"RESOLVED".equals(c.get("lifecycle"))){c.put("lifecycle","RESOLVED");c.put("capturedAt",now());put(text(c,"id"),"candidate",false,c);Map<String,Object> obs=new LinkedHashMap<>(c);obs.put("candidateId",c.get("id"));obs.put("change","RESOLVED");obs.put("differences",List.of(Map.of("field","lifecycle","before","ACTIVE","after","RESOLVED")));put(id("EVIDENCE"),"evidence",false,obs);changes.add(Map.of("candidateId",c.get("id"),"change","RESOLVED","reason","Absent from explicitly complete imported source snapshot"));}
  return put(scanId,"scan",false,Map.of("brandId",brandId,"platform",platform,"provenance",provenance,"capturedAt",now(),"changes",changes,"completeSourceSnapshot",Boolean.TRUE.equals(input.get("completeSourceSnapshot")),"coverage","Imported observations only; no live discovery connector"));
 }

 /** Evaluate submitted metadata against stored references; no external account fetch. */
 public Map<String,Object> analyzeSubmitted(Map<String,Object> input){
  String source=required(input,"sourceUrl");url(source);
  URI uri=URI.create(source);String host=uri.getHost().toLowerCase(Locale.ROOT);
  String platform=text(input,"platform");
  if(platform.isBlank()){
   if(host.equals("instagram.com")||host.endsWith(".instagram.com"))platform="Instagram";
   else if(host.equals("facebook.com")||host.endsWith(".facebook.com"))platform="Facebook";
   else if(host.equals("x.com")||host.equals("twitter.com"))platform="X";
   else if(host.equals("linkedin.com")||host.endsWith(".linkedin.com"))platform="LinkedIn";
   else if(host.equals("play.google.com"))platform="Google Play";
   else if(host.equals("apps.apple.com"))platform="Apple App Store";
   else platform="Website / other";
  }
  if(platform.length()>250)throw bad("Platform name too long",HttpStatus.BAD_REQUEST);
  String path=uri.getPath()==null?"":uri.getPath().replaceAll("/+$","");
  String slug=path.isEmpty()?host:path.substring(path.lastIndexOf('/')+1);
  String username=text(input,"username");if(username.isBlank())username=slug;
  String name=text(input,"name");if(name.isBlank())name=username.replaceAll("[_\\-.]+"," ").trim();
  if(name.isEmpty())name=host;
  if(name.length()>250||username.length()>250)throw bad("Name or username too long",HttpStatus.BAD_REQUEST);
  String identity=Similarity.normalize(name+" "+username+" "+host);
  // Compare common digit lookalikes as well as the literal supplied name.
  // This is an offline resemblance signal, not proof of ownership or fraud.
  String visualIdentity=identity.replace('0','o').replace('1','l').replace('3','e').replace('5','s');
  Map<String,Object> matched=null;int best=0;boolean ambiguous=false;
  List<Map<String,Object>> registeredAssets=list("asset");
  String cleanUrl=source.replaceAll("/+$","").toLowerCase(Locale.ROOT);
  for(Map<String,Object> brand:list("brand")){
   String brandKey=Similarity.normalize(text(brand,"name"));
   if(brandKey.length()<4)continue;
   boolean officialUrl=registeredAssets.stream().anyMatch(a->text(brand,"id").equals(a.get("brandId"))&&cleanUrl.equals(text(a,"sourceUrl").replaceAll("/+$","").toLowerCase(Locale.ROOT)));
   boolean similar=identity.contains(brandKey)||visualIdentity.contains(brandKey);
   if(!officialUrl&&!similar)continue;
   int confidence=(officialUrl?10000:0)+brandKey.length();
   if(confidence>best){best=confidence;matched=brand;ambiguous=false;}
   else if(confidence==best)ambiguous=true;
  }
  if(ambiguous)matched=null;
  String brandId=matched==null?"":text(matched,"id");
  String stable=cleanUrl;
  String provenance=Boolean.TRUE.equals(input.get("synthetic"))?"SYNTHETIC":"MANUAL_IMPORT";
  Map<String,Object> previous=null;
  for(Map<String,Object> old:list("candidate"))if(stable.equals(text(old,"identifier"))&&platform.equals(text(old,"platform"))&&provenance.equals(text(old,"provenance"))&&!isDemoSeed(text(old,"id"))){previous=old;break;}
  String candidateId=previous==null?id("CAND"):text(previous,"id");
  String scanId=id("SCAN");
  Map<String,Object> c=new LinkedHashMap<>();
  for(String field:List.of("publisher","description","logo","contact","outboundUrl")){
   String value=text(input,field);if(value.length()>4000)throw bad("Metadata too long",HttpStatus.BAD_REQUEST);c.put(field,value);
  }
  c.putAll(Map.of("brandId",brandId,"platform",platform,"identifier",stable,"name",name,"username",username,"sourceUrl",source,"provenance",provenance,"capturedAt",now(),"scanId",scanId,"lifecycle","ACTIVE"));
  Map<String,Object> assessment;
  if(matched!=null){assessment=risk(c);}else{
   int score=0;List<Map<String,Object>> signals=new ArrayList<>();
   String body=(text(c,"description")+" "+name).toLowerCase(Locale.ROOT);
   if(body.matches(".*(claim.{0,12}(reward|prize|coupon)|send.{0,8}otp|verify.{0,12}(account|password)|urgent payment).*")){
    score+=35;signals.add(Map.of("signal","Potential scam or credential request wording","points",35));
   }
   if((host+" "+path).matches(".*(secure-login|verify-account|claim-reward|free-coupon|reset-password).*")){
    score+=35;signals.add(Map.of("signal","Suspicious URL wording","points",35));
   }
   if(!text(c,"outboundUrl").isBlank()){
    score+=10;signals.add(Map.of("signal","Submitted outbound link requires separate verification","points",10));
   }
   assessment=new LinkedHashMap<>();assessment.put("score",score);
   assessment.put("verdict",score>=70?"LIKELY_IMPERSONATION":score>=35?"SUSPICIOUS":"INCONCLUSIVE");
   assessment.put("riskLevel",score>=70?"HIGH":score>=35?"MEDIUM":"UNKNOWN");
   assessment.put("safetyAssessment","NOT_VERIFIED_SAFE");assessment.put("officialExcluded",false);
   assessment.put("signals",signals);
   assessment.put("missing",List.of("No matching registered brand","No live account inspection","No external reputation verification"));
  }
  // Additional offline website-hostname indicators for the submitted URL.
  // Official matches are excluded from these warnings. These points indicate
  // patterns for an analyst to review; they are NOT a probability of fraud.
  if(websitePlatform(platform)
      && !Boolean.TRUE.equals(assessment.get("officialExcluded"))){
   String[] labels=host.split("\\.");
   boolean loginLabel=Arrays.stream(labels).anyMatch(label ->
       label.matches("(?i).*(login|signin|verify|secure|account|password|otp).*"));
   boolean digitInSubdomain=false;
   for(int i=0;i<labels.length-2;i++){
    if(labels[i].matches(".*\\d.*")){digitInSubdomain=true;break;}
   }
   @SuppressWarnings("unchecked")
   List<Map<String,Object>> signals=new ArrayList<>((List<Map<String,Object>>)assessment.get("signals"));
   int extra=0;
   if(loginLabel&&digitInSubdomain){
    extra+=40;
    signals.add(Map.of("signal","Login-themed host with a digit-containing subdomain",
        "points",40,"detail","Offline hostname pattern; no site contents inspected"));
   }
   if(matched!=null){
    String brandKey=Similarity.normalize(text(matched,"name"));
    if(!identity.contains(brandKey)&&visualIdentity.contains(brandKey)){
     extra+=35;
     signals.add(Map.of("signal","Digit-substituted resemblance to registered brand",
         "points",35,"detail","Common visual substitutions in supplied URL/name"));
    }
    boolean knownOfficialWebsite=false;
    boolean matchesOfficialHost=false;
    for(Map<String,Object> reference:registeredAssets){
     if(!text(matched,"id").equals(text(reference,"brandId"))
         ||!"Website".equalsIgnoreCase(text(reference,"platform")))continue;
     try{
      URI official=URI.create(text(reference,"sourceUrl"));
      if(official.getHost()==null)continue;
      String officialHost=official.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");
      knownOfficialWebsite=true;
      if(host.equals(officialHost)||host.endsWith("."+officialHost))matchesOfficialHost=true;
     }catch(IllegalArgumentException ignored){ /* Ignore malformed saved references. */ }
    }
    if(knownOfficialWebsite&&!matchesOfficialHost){
     extra+=20;
     signals.add(Map.of("signal","Different host from registered official website",
         "points",20,"detail","Compared hostname to supplied official website reference"));
    }
   }
   if(extra>0){
    int newScore=Math.min(100,((Number)assessment.get("score")).intValue()+extra);
    assessment.put("score",newScore);
    assessment.put("signals",signals);
    assessment.put("verdict",newScore>=70?"LIKELY_IMPERSONATION":newScore>=35?"SUSPICIOUS":"INCONCLUSIVE");
    assessment.put("riskLevel",newScore>=70?"HIGH":newScore>=35?"MEDIUM":"LOW");
    assessment.put("safetyAssessment",newScore>=35?"POTENTIAL_RISK":"INSUFFICIENT_EVIDENCE_TO_DECLARE_SAFE");
   }
  }
  if(websitePlatform(platform)){
   assessment.put("formula",String.valueOf(assessment.getOrDefault("formula",
       "Scam wording 35 + URL wording 35 + submitted outbound link 10"))
       + "; website-only extras: login-themed digit subdomain 40 + visual digit substitution 35"
       + " + off-reference website host 20; final score capped at 100");
  }
  c.put("risk",assessment);c.put("analystStatus",decision(candidateId));
  put(candidateId,"candidate",false,c);
  Map<String,Object> evidence=new LinkedHashMap<>(c);evidence.put("candidateId",candidateId);
  evidence.put("change",previous==null?"NEW":"REANALYZED");evidence.put("matchingSignals",assessment.get("signals"));
  evidence.put("screenshotStatus","UNAVAILABLE — no live capture");
  put(id("EVIDENCE"),"evidence",false,evidence);
  put(scanId,"scan",false,Map.of("candidateId",candidateId,"capturedAt",now(),"provenance",provenance,"coverage","Submitted metadata only; no live source lookup"));
  Map<String,Object> response=new LinkedHashMap<>();
  response.put("id",candidateId);response.put("name",name);response.put("sourceUrl",source);
  response.put("platform",platform);response.put("risk",assessment);
  response.put("matchedBrand",matched==null?"No unambiguous registered match":text(matched,"name"));
  response.put("provenance",provenance);response.put("coverage","Compared submitted metadata with stored references. No website or social account was fetched.");
  return response;
 }
 public List<Map<String,Object>> candidates(){return list("candidate").stream().map(c->{c.put("analystStatus",decision(text(c,"id")));return c;}).toList();}
 public Map<String,Object> comparison(String candidateId,String assetId){Map<String,Object> c=get(candidateId,"candidate"),a=get(assetId,"asset");if(!text(c,"brandId").equals(a.get("brandId")))throw bad("Compare an official asset from the same brand",HttpStatus.BAD_REQUEST);List<Map<String,Object>> fields=new ArrayList<>();for(String f:List.of("username","name","logo","description","publisher","identifier","platform"))fields.add(Map.of("field",f,"official",text(a,f),"candidate",text(c,f),"different",!text(a,f).equals(text(c,f)),"available",!text(a,f).isEmpty()&&!text(c,f).isEmpty()));return Map.of("official",a,"candidate",c,"fields",fields,"risk",c.get("risk"),"cases",list("case").stream().filter(k->candidateId.equals(k.get("candidateId"))).toList());}
 public Map<String,Object> newCase(String candidateId,Map<String,Object> input){get(candidateId,"candidate");return put(id("CASE"),"case",false,Map.of("candidateId",candidateId,"title",required(input,"title"),"createdAt",now(),"status","OPEN"));}
 public Map<String,Object> report(String caseId){Map<String,Object> c=get(caseId,"case");String cid=text(c,"candidateId");return Map.of("generatedAt",now(),"case",c,"candidate",get(cid,"candidate"),"evidence",list("evidence").stream().filter(e->cid.equals(e.get("candidateId"))).toList(),"investigationHistory",list("decision").stream().filter(e->cid.equals(e.get("candidateId"))).toList(),"limits","Evidence records contain source attribution. SYNTHETIC and MANUAL_IMPORT are not live monitoring. Screenshots unavailable. Analyst identity is a local development label.");}
 private String outboundHost(Map<String,Object> c){try{URI u=URI.create(text(c,"outboundUrl"));return List.of("http","https").contains(u.getScheme())&&u.getHost()!=null?u.getHost().toLowerCase(Locale.ROOT):"";}catch(Exception e){return "";}}
 public List<Map<String,Object>> clusters(){List<Map<String,Object>> all=candidates(),edges=new ArrayList<>();for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++){Map<String,Object>a=all.get(i),b=all.get(j);if(!a.get("brandId").equals(b.get("brandId"))||a.get("platform").equals(b.get("platform"))||!a.get("provenance").equals(b.get("provenance")))continue;List<String> reasons=new ArrayList<>();String contact=text(a,"contact");if(!contact.isEmpty()&&contact.equalsIgnoreCase(text(b,"contact")))reasons.add("Same supplied contact: "+contact);String host=outboundHost(a);if(!host.isEmpty()&&host.equals(outboundHost(b))&&!List.of("facebook.com","instagram.com","x.com","t.co","youtube.com","play.google.com","apps.apple.com","linktr.ee").contains(host))reasons.add("Same supplied outbound host: "+host);if(!reasons.isEmpty())edges.add(Map.of("from",a.get("id"),"to",b.get("id"),"reasons",reasons,"provenance",a.get("provenance"),"claim","Shared observed indicators; common ownership unproven"));}
  List<Map<String,Object>> groups=new ArrayList<>();Set<String> visited=new HashSet<>();for(Map<String,Object> edge:edges){String start=text(edge,"from");if(visited.contains(start))continue;Set<String> members=new LinkedHashSet<>();members.add(start);boolean grew=true;while(grew){grew=false;for(Map<String,Object> e:edges)if(members.contains(text(e,"from"))||members.contains(text(e,"to"))){grew|=members.add(text(e,"from"));grew|=members.add(text(e,"to"));}}visited.addAll(members);groups.add(Map.of("members",all.stream().filter(c->members.contains(c.get("id"))).toList(),"relationships",edges.stream().filter(e->members.contains(e.get("from"))).toList(),"claim","Evidence grouping only; not an ownership attribution"));}return groups;
 }
 public Map<String,Object> exposure(String brandId){get(brandId,"brand");List<Map<String,Object>> observed=candidates().stream().filter(c->brandId.equals(c.get("brandId"))).toList();Map<String,Object> scores=new LinkedHashMap<>();for(String provenance:List.of("SYNTHETIC","MANUAL_IMPORT")){List<Map<String,Object>> active=observed.stream().filter(c->provenance.equals(c.get("provenance"))&&"ACTIVE".equals(c.get("lifecycle"))&&!Boolean.TRUE.equals(((Map<?,?>)c.get("risk")).get("officialExcluded"))&&!List.of("DISMISSED","ALLOWLISTED").contains(c.get("analystStatus"))).toList();double sum=active.stream().mapToDouble(c->((Number)((Map<?,?>)c.get("risk")).get("score")).doubleValue()).sum();scores.put(provenance,Map.of("score",Math.round(100*(1-Math.exp(-sum/200))),"sumRisk",sum,"count",active.size()));}return Map.of("scores",scores,"formula","round(100 × (1 − exp(−Σ active candidate risk / 200))); dismissed, allowlisted and resolved excluded; provenance scored separately","configuredLiveSources",0,"liveScore","UNAVAILABLE","observedPlatforms",observed.stream().map(c->c.get("platform")).distinct().toList(),"coverage","Imported/synthetic observations only. Missing data is unknown, never evidence of safety. Does not measure the entire internet.");}
 public Map<String,Object> overview(){
  List<Map<String,Object>> active=candidates().stream().filter(c->"ACTIVE".equals(c.get("lifecycle"))&&!text(c,"id").startsWith("DEMO-")).toList();
  long high=active.stream().filter(c->((Number)((Map<?,?>)c.get("risk")).get("score")).intValue()>=70).count();
  long medium=active.stream().filter(c->{int score=((Number)((Map<?,?>)c.get("risk")).get("score")).intValue();return score>=35&&score<70;}).count();
  Map<String,Long> byPlatform=new TreeMap<>();
  for(Map<String,Object> c:active)byPlatform.merge(text(c,"platform"),1L,Long::sum);
  return Map.of("total",active.size(),"high",high,"medium",medium,"low",active.size()-high-medium,"platformCounts",byPlatform,"coverage","Submitted data only; no live discovery connector");
 }
 public Map<String,Object> reset(Map<String,Object> input){if(!"RESET SYNTHETIC DEMO".equals(input.get("confirmation")))throw bad("Type RESET SYNTHETIC DEMO",HttpStatus.BAD_REQUEST);List<RecordDocument> designated=store.findByDemoSeedTrue();store.deleteAll(designated);store.flush();seed();put(id("AUDIT"),"resetAudit",false,Map.of("createdAt",now(),"action","RESET_DESIGNATED_DEMO_SEEDS","deleted",designated.size(),"actor","LOCAL_ANALYST"));return Map.of("message","Designated synthetic seeds restored. User assets, scans, candidates, cases and audit history preserved.","count",designated.size());}
 public void seed(){if(store.existsById("DEMO-BRAND"))return;put("DEMO-BRAND","brand",true,Map.of("name","NovaBank","provenance","SYNTHETIC","createdAt",now()));
  for(String platform:List.of("Instagram","Google Play")){String key=platform.equals("Instagram")?"DEMO-SOCIAL-ASSET":"DEMO-APP-ASSET";put(key,"asset",true,Map.of("brandId","DEMO-BRAND","name","NovaBank","username","novabank","platform",platform,"identifier",platform.equals("Instagram")?"novabank-official":"com.novabank.official","publisher","NovaBank Ltd","logo","https://novabank.example/logo.svg","description","Official banking services","sourceUrl","https://novabank.example/official","provenance","SYNTHETIC"));}
  for(int i=0;i<3;i++){String key="DEMO-CAND-"+i;Map<String,Object> c=new LinkedHashMap<>();c.putAll(Map.of("brandId","DEMO-BRAND","name",i==0?"NоvaBank":i==1?"NovaBank Rewards":"Nova Garden","username",i==0?"nova_bank_help":"novabank_rewards","platform",i==1?"Google Play":"Instagram","identifier","synthetic-candidate-"+i,"publisher",i==1?"Reward Apps":"","sourceUrl","https://candidate"+i+".example/profile","logo",i<2?"https://novabank.example/logo.svg":""));c.putAll(Map.of("description",i<2?"Claim reward; send OTP":"Gardening community","contact",i<2?"help@reward.example":"","outboundUrl",i<2?"https://reward.example/claim":"","provenance","SYNTHETIC","capturedAt",now(),"lifecycle","ACTIVE"));c.put("risk",risk(c));put(key,"candidate",true,c);Map<String,Object> e=new LinkedHashMap<>(c);e.put("candidateId",key);e.put("change","NEW");e.put("differences",List.of());e.put("screenshotStatus","UNAVAILABLE — synthetic fixture");e.put("matchingSignals",((Map<?,?>)c.get("risk")).get("signals"));put("DEMO-EVIDENCE-"+i,"evidence",true,e);}
 }
}
