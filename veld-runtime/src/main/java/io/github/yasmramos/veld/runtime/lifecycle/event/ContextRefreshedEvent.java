/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.runtime.lifecycle.event;

/**
 * Event published when the container has been initialized or refreshed.
 *
 * <p>This event is raised when:
 * <ul>
 *   <li>All bean definitions have been loaded</li>
 *   <li>All singleton beans have been instantiated</li>
 *   <li>All {@code @PostConstruct} methods have been called</li>
 *   <li>All {@code @PostInitialize} methods have been called</li>
 * </ul>
 *
 * <p>This is the ideal point to perform tasks that require all beans
 * to be available, such as cache warming or starting background tasks.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class ApplicationStartup {
 *     
 *     @Inject
 *     private CacheService cacheService;
 *     
 *     @Subscribe
 *     public void onContextRefreshed(ContextRefreshedEvent event) {
 *         System.out.println("Application ready!");
 *         System.out.println("Beans loaded: " + event.getBeanCount());
 *         cacheService.warmUp();
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see ContextStartedEvent
 */
public class ContextRefreshedEvent extends ContextEvent {
    
    private final int beanCount;
    private final long initializationTimeMs;
    
    /**
     * Creates a new context refreshed event.
     *
     * @param source the source of the event
     * @param beanCount the number of beans in the container
     * @param initializationTimeMs time taken to initialize in milliseconds
     */
    public ContextRefreshedEvent(Object source, int beanCount, long initializationTimeMs) {
        super(source);
        this.beanCount = beanCount;
        this.initializationTimeMs = initializationTimeMs;
    }
    
    /**
     * Returns the number of beans registered in the container.
     *
     * @return the bean count
     */
    public int getBeanCount() {
        return beanCount;
    }
    
    /**
     * Returns the time taken to initialize the container.
     *
     * @return initialization time in milliseconds
     */
    public long getInitializationTimeMs() {
        return initializationTimeMs;
    }
    
    @Override
    public String toString() {
        return "ContextRefreshedEvent{" +
                "beanCount=" + beanCount +
                ", initializationTimeMs=" + initializationTimeMs +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
