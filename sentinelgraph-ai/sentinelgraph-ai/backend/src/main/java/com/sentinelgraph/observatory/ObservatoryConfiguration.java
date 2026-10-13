package com.sentinelgraph.observatory;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
@Configuration
public class ObservatoryConfiguration {
 @Bean ApplicationRunner observatorySeed(ObservatoryService service){return args->service.seed();}
}
