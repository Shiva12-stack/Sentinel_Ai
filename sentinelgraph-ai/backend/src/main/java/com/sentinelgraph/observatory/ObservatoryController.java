package com.sentinelgraph.observatory;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/observatory")
public class ObservatoryController {
 private final ObservatoryService service;
 public ObservatoryController(ObservatoryService service){this.service=service;}
 @GetMapping public Map<String,Object> overview(){return service.overview();}
 @PostMapping("/brands") public Map<String,Object> brand(@RequestBody Map<String,Object> body){return service.brand(body);}
 @PostMapping("/assets") public Map<String,Object> asset(@RequestBody Map<String,Object> body){return service.asset(body);}
 @PostMapping("/test") public Map<String,Object> test(@RequestBody Map<String,Object> body){return service.test(body);}
 @PostMapping("/analyze") public Map<String,Object> autoAnalyze(@RequestBody Map<String,Object> body){return service.analyzeSubmitted(body);}
 @PostMapping("/scans") public Map<String,Object> scan(@RequestBody Map<String,Object> body){return service.scan(body);}
 @GetMapping("/evidence") public List<Map<String,Object>> evidence(){return service.list("evidence");}
 @GetMapping("/history") public List<Map<String,Object>> history(){return service.list("evidence");}
 @GetMapping("/decisions") public List<Map<String,Object>> decisions(){return service.list("decision");}
 @PostMapping("/candidates/{id}/feedback") public Map<String,Object> feedback(@PathVariable String id,@RequestBody Map<String,Object> body){return service.feedback(id,body);}
 @PostMapping("/candidates/{id}/cases") public Map<String,Object> newCase(@PathVariable String id,@RequestBody Map<String,Object> body){return service.newCase(id,body);}
 @GetMapping("/compare") public Map<String,Object> compare(@RequestParam String candidateId,@RequestParam String assetId){return service.comparison(candidateId,assetId);}
 @GetMapping("/clusters") public List<Map<String,Object>> clusters(){return service.clusters();}
 @GetMapping("/brands/{id}/exposure") public Map<String,Object> exposure(@PathVariable String id){return service.exposure(id);}
 @GetMapping("/cases/{id}/report") public Map<String,Object> report(@PathVariable String id){return service.report(id);}
 @PostMapping("/demo/reset") public Map<String,Object> reset(@RequestBody Map<String,Object> body){return service.reset(body);}
}
