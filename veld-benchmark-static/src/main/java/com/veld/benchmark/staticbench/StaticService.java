package com.veld.benchmark.staticbench;

import com.veld.annotations.Singleton;
import javax.inject.Inject;

/**
 * Simple service for static accessor benchmark.
 * Annotated with Veld's @Singleton for code generation.
 */
@Singleton
public class StaticService {
    
    private final String value = "StaticService";
    
    @Inject
    public StaticService() {
    }
    
    public String getValue() {
        return value;
    }
}
