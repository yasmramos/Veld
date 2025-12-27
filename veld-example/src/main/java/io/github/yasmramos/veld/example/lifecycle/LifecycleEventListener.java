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
package io.github.yasmramos.veld.example.lifecycle;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.runtime.lifecycle.event.*;

import java.time.Instant;

/**
 * Example of listening to container lifecycle events via EventBus.
 */
@Singleton
public class LifecycleEventListener {
    
    private Instant startTime;
    
    @Subscribe
    public void onContextRefreshed(ContextRefreshedEvent event) {
        System.out.println("  [LifecycleEventListener] ContextRefreshedEvent received:");
        System.out.println("    - Beans loaded: " + event.getBeanCount());
        System.out.println("    - Init time: " + event.getInitializationTimeMs() + "ms");
        System.out.println("    - Timestamp: " + event.getTimestamp());
    }
    
    @Subscribe
    public void onContextStarted(ContextStartedEvent event) {
        startTime = event.getTimestamp();
        System.out.println("  [LifecycleEventListener] ContextStartedEvent received:");
        System.out.println("    - Lifecycle beans: " + event.getLifecycleBeanCount());
        System.out.println("    - Start time: " + startTime);
    }
    
    @Subscribe
    public void onContextStopped(ContextStoppedEvent event) {
        System.out.println("  [LifecycleEventListener] ContextStoppedEvent received:");
        System.out.println("    - Lifecycle beans stopped: " + event.getLifecycleBeanCount());
    }
    
    @Subscribe
    public void onContextClosed(ContextClosedEvent event) {
        System.out.println("  [LifecycleEventListener] ContextClosedEvent received:");
        System.out.println("    - Uptime: " + event.getUptime());
        System.out.println("    - Beans destroyed: " + event.getDestroyedBeanCount());
    }
}
