package com.sentinelgraph.observatory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RecordStore extends JpaRepository<RecordDocument,String> {
 List<RecordDocument> findByKind(String kind);
 List<RecordDocument> findByDemoSeedTrue();
}
