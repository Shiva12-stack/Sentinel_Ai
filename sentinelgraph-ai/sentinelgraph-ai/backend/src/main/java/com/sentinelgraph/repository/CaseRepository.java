package com.sentinelgraph.repository;

import com.sentinelgraph.model.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaseRepository extends JpaRepository<InvestigationCase,String> {
    List<InvestigationCase> findAllByOrderByCreatedAtDesc();
    long countByStatusNotIn(List<String> status);
}
