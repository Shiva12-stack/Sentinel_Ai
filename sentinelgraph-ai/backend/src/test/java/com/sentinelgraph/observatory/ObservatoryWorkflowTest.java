package com.sentinelgraph.observatory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:obs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop"})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
class ObservatoryWorkflowTest {
 @Autowired ObservatoryService service;
 @Autowired RecordStore store;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 Map<String,Object> candidate(String name,String identifier){return Map.of("name",name,"identifier",identifier,"sourceUrl","https://candidate.example/account","description","Claim reward; send OTP","contact","same@threat.example","outboundUrl","https://threat.example");}
 Map<String,Object> scan(String platform,String name,String identifier,boolean complete){return service.scan(Map.of("brandId","DEMO-BRAND","platform",platform,"provenance","MANUAL_IMPORT","completeSourceSnapshot",complete,"candidates",List.of(candidate(name,identifier))));}
 String cid(String identifier){return String.valueOf(service.candidates().stream().filter(c->identifier.equals(c.get("identifier"))).findFirst().orElseThrow().get("id"));}
 @Test void apiTestRouteAndResetValidation() throws Exception {mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/observatory/test").contentType("application/json").content("{\"brandName\":\"NovaBank\",\"candidateName\":\"Nova Bank\"}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.similarity").value(100.0));mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/observatory/demo/reset").contentType("application/json").content("{\"confirmation\":\"wrong\"}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());}
 @Test void manualTestsAreNotMonitoringAndExplainConfusables(){long before=store.count();var r=service.test(Map.of("brandName","NovaBank","candidateName","NоvaBank"));assertEquals(100.0,r.get("similarity"));assertFalse(((List<?>)r.get("rawDifferences")).isEmpty());assertEquals(before,store.count());for(String name:List.of("Nova Bank","N0vaBank","NovaBank Support"))assertNotNull(service.test(Map.of("brandName","NovaBank","candidateName",name)).get("similarity"));}
 @Test void officialIdentifiersExcludedAndShortNamesSafeguarded(){scan("Instagram","NovaBank","novabank-official",false);var c=service.get(cid("novabank-official"),"candidate");assertEquals(0,((Map<?,?>)c.get("risk")).get("score"));assertEquals("SUPPLIED_OFFICIAL_ASSET_MATCH",((Map<?,?>)c.get("risk")).get("verdict"));assertEquals(true,((Map<?,?>)c.get("risk")).get("officialExcluded"));var r=service.test(Map.of("brandName","NovaBank","candidateName","NovaBank","platform","Instagram","identifier","novabank-official"));assertEquals(true,r.get("officialExcluded"));var brand=service.brand(Map.of("name","Ab"));service.scan(Map.of("brandId",brand.get("id"),"platform","X","provenance","MANUAL_IMPORT","candidates",List.of(Map.of("name","Ab Help","identifier","short-id","sourceUrl","https://short.example"))));assertEquals(0,((Map<?,?>)service.get(cid("short-id"),"candidate").get("risk")).get("score"));}
 @Test void scanExposesConsistentRiskClassification(){
  var result=scan("Instagram","NovaBank Help","risk-check",false);
  var change=(Map<?,?>)((List<?>)result.get("changes")).get(0);
  var risk=(Map<?,?>)change.get("risk");
  assertTrue(risk.get("score") instanceof Number);
  assertNotNull(risk.get("verdict"));
  assertNotNull(risk.get("riskLevel"));
  assertNotNull(risk.get("safetyAssessment"));
  assertTrue(((Number)risk.get("score")).intValue()>0);
 }
 @Test void evidenceSnapshotsComparisonCasesAndReport(){scan("Instagram","NovaBank Help","case-target",false);String cid=cid("case-target");service.feedback(cid,Map.of("status","CONFIRMED","note","Credential bait observed"));var c=service.newCase(cid,Map.of("title","Review account"));var report=service.report(String.valueOf(c.get("id")));assertFalse(((List<?>)report.get("evidence")).isEmpty());assertEquals(1,((List<?>)report.get("investigationHistory")).size());var comparison=service.comparison(cid,"DEMO-SOCIAL-ASSET");assertEquals(7,((List<?>)comparison.get("fields")).size());var e=(Map<?,?>)((List<?>)report.get("evidence")).get(0);assertEquals("MANUAL_IMPORT",e.get("provenance"));assertNotNull(e.get("capturedAt"));assertNotNull(e.get("sourceUrl"));assertNotNull(e.get("screenshotStatus"));}
 @Test void changedAndResolvedRequireCompleteSnapshot(){scan("Instagram","NovaBank Help","history-target",false);scan("Instagram","NovaBank Rewards","history-target",false);String cid=cid("history-target");service.scan(Map.of("brandId","DEMO-BRAND","platform","Instagram","provenance","MANUAL_IMPORT","candidates",List.of()));assertEquals("ACTIVE",service.get(cid,"candidate").get("lifecycle"));service.scan(Map.of("brandId","DEMO-BRAND","platform","Instagram","provenance","MANUAL_IMPORT","completeSourceSnapshot",true,"candidates",List.of()));assertEquals("RESOLVED",service.get(cid,"candidate").get("lifecycle"));assertTrue(service.list("evidence").stream().anyMatch(e->"CHANGED".equals(e.get("change"))&&!((List<?>)e.get("differences")).isEmpty()));}
 @Test void nameAloneNeverClusters(){service.scan(Map.of("brandId","DEMO-BRAND","platform","X","provenance","MANUAL_IMPORT","candidates",List.of(Map.of("name","NovaBank Help","identifier","no-edge","sourceUrl","https://x.example/u"))));assertTrue(service.clusters().stream().noneMatch(g->((List<Map<String,Object>>)g.get("members")).stream().anyMatch(c->"no-edge".equals(c.get("identifier")))));scan("X","NovaBank","edge-x",false);scan("Facebook","NovaBank","edge-fb",false);assertTrue(service.clusters().stream().anyMatch(g->((List<?>)g.get("relationships")).size()>0&&"MANUAL_IMPORT".equals(((Map<?,?>)((List<?>)g.get("relationships")).get(0)).get("provenance"))));}
 @Test void analystAllowlistPersistsAcrossScansAndAffectsExposure(){scan("Instagram","NovaBank Help","allow-target",false);String cid=cid("allow-target");service.feedback(cid,Map.of("status","ALLOWLISTED","note","Authorized partner"));scan("Instagram","NovaBank Rewards","allow-target",false);assertEquals("ALLOWLISTED",service.candidates().stream().filter(c->cid.equals(c.get("id"))).findFirst().orElseThrow().get("analystStatus"));var exposure=service.exposure("DEMO-BRAND");var manual=(Map<?,?>)((Map<?,?>)exposure.get("scores")).get("MANUAL_IMPORT");assertEquals(0,manual.get("count"));assertEquals(0L,manual.get("score"));}
 @Test void demoResetPreservesAllUserRecordsAndAudit(){var brand=service.brand(Map.of("name","Real Brand"));scan("Instagram","NovaBank User Synthetic","user-record",false);String cid=cid("user-record");service.feedback(cid,Map.of("status","DISMISSED","note","False positive"));var c=service.newCase(cid,Map.of("title","Keep case"));service.reset(Map.of("confirmation","RESET SYNTHETIC DEMO"));assertNotNull(service.get(String.valueOf(brand.get("id")),"brand"));assertNotNull(service.get(cid,"candidate"));assertNotNull(service.report(String.valueOf(c.get("id"))));assertEquals(1,service.list("decision").stream().filter(d->cid.equals(d.get("candidateId"))).count());assertEquals(1,service.list("resetAudit").size());assertThrows(ResponseStatusException.class,()->service.reset(Map.of("confirmation","")));}
 @Test void userSyntheticRecordCannotBecomeResettableSeed(){service.scan(Map.of("brandId","DEMO-BRAND","platform","Instagram","provenance","SYNTHETIC","candidates",List.of(candidate("User synthetic modification","synthetic-candidate-0"))));var user=service.candidates().stream().filter(c->"User synthetic modification".equals(c.get("name"))).findFirst().orElseThrow();assertFalse(store.findById(String.valueOf(user.get("id"))).orElseThrow().demoSeed);service.reset(Map.of("confirmation","RESET SYNTHETIC DEMO"));assertNotNull(service.get(String.valueOf(user.get("id")),"candidate"));}
 @Test void refusesInventedLiveSourcesAndBadInput(){assertThrows(ResponseStatusException.class,()->service.scan(Map.of("brandId","DEMO-BRAND","platform","Instagram","provenance","LIVE","candidates",List.of())));assertThrows(ResponseStatusException.class,()->service.scan(Map.of("brandId","DEMO-BRAND","platform","Instagram","provenance","MANUAL_IMPORT","candidates",List.of(Map.of("name","Name","identifier","x","sourceUrl","javascript:alert(1)")))));}

 @Test void automaticAnalysisMatchesRegisteredBrandWithoutSelectingIt(){
  var brand=service.brand(Map.of("name","FoodRush Testing"));
  service.asset(Map.of("brandName","FoodRush Testing","platform","Instagram","identifier","foodrushtesting_official","name","FoodRush Testing Official","sourceUrl","https://www.instagram.com/foodrushtesting_official/"));
  var result=service.analyzeSubmitted(Map.of("sourceUrl","https://www.instagram.com/foodrushtesting_reward_demo/","name","FoodRush Testing Claim Reward","description","Claim reward; verify account","synthetic",true));
  assertEquals("FoodRush Testing",result.get("matchedBrand"));
  assertTrue(((Number)((Map<?,?>)result.get("risk")).get("score")).intValue()>=35);
  var again=service.analyzeSubmitted(Map.of("sourceUrl","https://www.instagram.com/foodrushtesting_reward_demo/","name","FoodRush Testing Claim Reward","synthetic",true));
  assertEquals(result.get("id"),again.get("id"));
 }
 @Test void unknownReferenceDoesNotClaimVerifiedSafety(){
  var result=service.analyzeSubmitted(Map.of("sourceUrl","https://no-brand-reference.example/unknown","synthetic",true));
  assertEquals("No unambiguous registered match",result.get("matchedBrand"));
  assertEquals("INCONCLUSIVE",((Map<?,?>)result.get("risk")).get("verdict"));
  assertEquals("NOT_VERIFIED_SAFE",((Map<?,?>)result.get("risk")).get("safetyAssessment"));
 }
}
