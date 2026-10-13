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
    @Test void detectsFictionalReference(){AnalysisResponse r=service.analyze("shopkart.example");assertEquals("VERIFIED_REFERENCE_MATCH",r.verdict());assertEquals("NOT_CHECKED",r.threatStatus());}
    @Test void flagsLookalike(){AnalysisResponse r=service.analyze("shopkart-secure-login.example");assertEquals("SUSPICIOUS",r.verdict());}
    @Test void handlesUnknown(){AnalysisResponse r=service.analyze("unknown-service.example");assertEquals("INCONCLUSIVE",r.verdict());}
    @Test void extractsActualDomain(){AnalysisResponse r=service.analyze("paypal.com.attacker.example");assertEquals("attacker.example",r.registrableDomain());}
    @Test void blocksEmbeddedCredentials(){assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.analyze("https://user:password@example.com"));}
}
