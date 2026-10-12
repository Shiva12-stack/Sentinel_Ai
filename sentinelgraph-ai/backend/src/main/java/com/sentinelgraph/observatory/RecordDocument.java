package com.sentinelgraph.observatory;
import jakarta.persistence.*;
@Entity @Table(name="observatory_records",indexes=@Index(name="obs_kind",columnList="kind"))
public class RecordDocument {
 @Id public String id;
 @Column(nullable=false) public String kind;
 @Column(nullable=false) public boolean demoSeed;
 @Column(nullable=false,columnDefinition="text") public String payload;
 @Version public Long version;
 public RecordDocument(){}
 public RecordDocument(String id,String kind,boolean seed,String payload){this.id=id;this.kind=kind;this.demoSeed=seed;this.payload=payload;}
}
