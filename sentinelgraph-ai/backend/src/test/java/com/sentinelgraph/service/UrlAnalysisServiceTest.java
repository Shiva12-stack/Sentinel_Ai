package com.sentinelgraph.service;

import com.sentinelgraph.dto.AnalysisDtos.AnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:legacy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop"})
class UrlAnalysisServiceTest {
    @Autowired UrlAnalysisService service;
    @Test void detectsFictionalReference(){AnalysisResponse r=service.analyze("shopkart.example");assertEquals("VERIFIED_REFERENCE_MATCH",r.verdict());assertNotNull(r.threatStatus());assertEquals(0,r.score());assertEquals("REFERENCE_MATCH_NOT_SAFETY_PROOF",r.safetyAssessment());}
    @Test void flagsLookalike(){AnalysisResponse r=service.analyze("shopkart-secure-login.example");assertEquals("SUSPICIOUS",r.verdict());assertTrue(r.score()>=55);assertEquals("POTENTIAL_RISK",r.safetyAssessment());}
    @Test void handlesUnknown(){AnalysisResponse r=service.analyze("unknown-service.example");assertEquals("INCONCLUSIVE",r.verdict());assertNotNull(r.score());assertEquals("INSUFFICIENT_EVIDENCE_TO_DECLARE_SAFE",r.safetyAssessment());}
    @Test void extractsActualDomain(){AnalysisResponse r=service.analyze("paypal.com.attacker.example");assertEquals("attacker.example",r.registrableDomain());assertTrue(r.score()>=35);}
    @Test void catchesBrandInsideUnrelatedLoginDomain(){
        AnalysisResponse r=service.analyze("https://www.amazon.login.page.in");
        assertEquals("Amazon",r.claimedBrand());
        assertTrue(r.score()>=70);
        assertTrue(r.evidence().stream().anyMatch(e->e.ruleId().equals("BRAND_IN_UNRELATED_SUBDOMAIN")));
    }
    @Test void detectsDigitLookalikeWithoutRegisteringIt(){
        AnalysisResponse r=service.analyze("https://www.amaz0n12.in");
        assertTrue(r.score()>=70);
    }
    @Test void comparesCommonOfficialReferenceWithoutCallingItSafe(){
        AnalysisResponse r=service.analyze("https://www.amazon.in");
        assertEquals("VERIFIED_REFERENCE_MATCH",r.verdict());
        assertEquals(0,r.score());
        assertEquals("REFERENCE_MATCH_NOT_SAFETY_PROOF",r.safetyAssessment());
    }
    @Test void noSuspiciousSignalIsInconclusiveNotVerifiedSafe(){
        AnalysisResponse r=service.analyze("https://ordinary-site.example");
        assertEquals("INCONCLUSIVE",r.verdict());
        assertEquals("INSUFFICIENT_EVIDENCE_TO_DECLARE_SAFE",r.safetyAssessment());
    }
    @Test void blocksEmbeddedCredentials(){assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.analyze("https://user:password@example.com"));}
}
