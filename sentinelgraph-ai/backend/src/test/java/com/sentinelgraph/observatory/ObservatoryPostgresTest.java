package com.sentinelgraph.observatory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS",matches="true")
class ObservatoryPostgresTest {
 @Autowired JdbcTemplate jdbc;
 @Autowired ObservatoryService service;
 @Test void durablePostgresEvidenceAndReset(){assertTrue(jdbc.queryForObject("select version()",String.class).contains("PostgreSQL"));assertNotNull(service.overview());assertFalse(service.list("evidence").isEmpty());int before=service.list("resetAudit").size();service.reset(java.util.Map.of("confirmation","RESET SYNTHETIC DEMO"));assertEquals(before+1,service.list("resetAudit").size());}
}
