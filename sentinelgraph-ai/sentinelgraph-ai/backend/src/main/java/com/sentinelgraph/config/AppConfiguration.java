package com.sentinelgraph.config;

import com.sentinelgraph.repository.AnalysisRepository;
import com.sentinelgraph.service.UrlAnalysisService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfiguration {
    @Bean WebMvcConfigurer cors(){return new WebMvcConfigurer(){
        @Override public void addCorsMappings(CorsRegistry registry){registry.addMapping("/api/**").allowedOrigins("http://localhost:5173","http://127.0.0.1:5173").allowedMethods("GET","POST","OPTIONS");}
    };}
}
