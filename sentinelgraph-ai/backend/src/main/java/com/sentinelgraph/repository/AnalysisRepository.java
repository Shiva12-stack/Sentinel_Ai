package com.sentinelgraph.repository;

import com.sentinelgraph.model.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<AnalysisRecord,String> {
    List<AnalysisRecord> findAllByOrderByCreatedAtDesc();
    long countByVerdict(String verdict);
}
