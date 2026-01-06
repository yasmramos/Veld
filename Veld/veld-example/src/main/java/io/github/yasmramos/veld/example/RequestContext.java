package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Prototype;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import java.util.UUID;

/**
 * Request context - Prototype scope.
 * Demonstrates @Prototype - new instance created each time.
 * Uses field injection.
 * 
 * Note: @Prototype implies @Component, so we don't need both.
 */
@Prototype
public class RequestContext {
    
    private static int instanceCount = 0;
    
    @Inject
    LogService logService;  // Package-private for bytecode injection
    
    private final String requestId;
    private final int instanceNumber;
    private long timestamp;
    
    public RequestContext() {
        this.requestId = UUID.randomUUID().toString().substring(0, 8);
        this.instanceNumber = ++instanceCount;
        System.out.println("[RequestContext] Constructor called - Instance #" + instanceNumber);
    }
    
    @PostConstruct
    public void init() {
        this.timestamp = System.currentTimeMillis();
        logService.debug("RequestContext #" + instanceNumber + " created with ID: " + requestId);
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public int getInstanceNumber() {
        return instanceNumber;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void process(String action) {
        logService.log("Request[" + requestId + "] Processing action: " + action);
    }
    
    public static void resetInstanceCount() {
        instanceCount = 0;
    }
}
