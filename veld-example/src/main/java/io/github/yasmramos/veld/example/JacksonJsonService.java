package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.ConditionalOnClass;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * JSON service that uses Jackson for serialization.
 * Only activated when Jackson is available on the classpath.
 * 
 * This demonstrates @ConditionalOnClass - this service will only be
 * registered if the Jackson ObjectMapper class is present.
 */
@Singleton
@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonJsonService {
    
    @PostConstruct
    public void init() {
        System.out.println("[JacksonJsonService] Jackson library detected - using Jackson for JSON");
    }
    
    public String toJson(Object obj) {
        // In a real implementation, would use Jackson's ObjectMapper
        return "Jackson JSON: " + obj.toString();
    }
    
    public <T> T fromJson(String json, Class<T> type) {
        // In a real implementation, would use Jackson's ObjectMapper
        return null;
    }
}
