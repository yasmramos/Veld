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
package io.github.yasmramos.veld.runtime.lifecycle;

import io.github.yasmramos.veld.annotation.DependsOn;
import io.github.yasmramos.veld.annotation.OnStart;
import io.github.yasmramos.veld.annotation.OnStop;
import io.github.yasmramos.veld.annotation.PostInitialize;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.lifecycle.event.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of beans in the container.
 *
 * <p>This processor handles:
 * <ul>
 *   <li>Bean post-processing before and after initialization</li>
 *   <li>Lifecycle interface callbacks (start/stop)</li>
 *   <li>Annotation-based lifecycle callbacks (@OnStart, @OnStop, etc.)</li>
 *   <li>Publishing lifecycle events to EventBus</li>
 *   <li>Respecting initialization order (@DependsOn, phases)</li>
 * </ul>
 *
 * <h2>Lifecycle Phases</h2>
 * <ol>
 *   <li><b>Initialization</b>: Beans are created and dependencies injected</li>
 *   <li><b>Post-Processing</b>: BeanPostProcessors modify beans</li>
 *   <li><b>Post-Initialize</b>: @PostInitialize callbacks after all beans ready</li>
 *   <li><b>Start</b>: Lifecycle beans and @OnStart methods invoked</li>
 *   <li><b>Running</b>: Application is operational</li>
 *   <li><b>Stop</b>: Lifecycle beans and @OnStop methods invoked</li>
 *   <li><b>Destroy</b>: DisposableBean and @PreDestroy callbacks</li>
 * </ol>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 */
public class LifecycleProcessor {
    
    /**
     * Public constructor for instantiation by generated Veld class.
     */
    public LifecycleProcessor() {
    }
    
    private static final Logger LOGGER = Logger.getLogger(LifecycleProcessor.class.getName());
    
    private final List<BeanPostProcessor> postProcessors = new CopyOnWriteArrayList<>();
    private final Map<String, Object> managedBeans = new ConcurrentHashMap<>();
    private final Map<String, BeanLifecycleState> beanStates = new ConcurrentHashMap<>();
    private final List<LifecycleCallback> postInitializeCallbacks = new ArrayList<>();
    private final List<LifecycleCallback> onStartCallbacks = new ArrayList<>();
    private final List<LifecycleCallback> onStopCallbacks = new ArrayList<>();
    
    private EventBus eventBus;
    private Instant startTime;
    private volatile boolean running = false;
    
    /**
     * Bean lifecycle state tracking.
     */
    private enum BeanLifecycleState {
        CREATED,
        POST_PROCESSED,
        INITIALIZED,
        STARTED,
        STOPPED,
        DESTROYED
    }
    
    /**
     * Represents a lifecycle callback method.
     */
    private static class LifecycleCallback implements Comparable<LifecycleCallback> {
        final Object bean;
        final String beanName;
        final Method method;
        final int order;
        
        LifecycleCallback(Object bean, String beanName, Method method, int order) {
            this.bean = bean;
            this.beanName = beanName;
            this.method = method;
            this.order = order;
        }
        
        void invoke() throws Exception {
            method.setAccessible(true);
            method.invoke(bean);
        }
        
        @Override
        public int compareTo(LifecycleCallback other) {
            return Integer.compare(this.order, other.order);
        }
    }
    
    /**
     * Sets the EventBus for publishing lifecycle events.
     *
     * @param eventBus the event bus
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    /**
     * Registers a BeanPostProcessor.
     *
     * @param postProcessor the post processor to add
     */
    public void addBeanPostProcessor(BeanPostProcessor postProcessor) {
        postProcessors.add(postProcessor);
        postProcessors.sort(Comparator.comparingInt(BeanPostProcessor::getOrder));
        LOGGER.fine("Registered BeanPostProcessor: " + postProcessor.getClass().getSimpleName());
    }
    
    /**
     * Registers a bean for lifecycle management.
     *
     * @param beanName the bean name
     * @param bean the bean instance
     */
    public void registerBean(String beanName, Object bean) {
        managedBeans.put(beanName, bean);
        beanStates.put(beanName, BeanLifecycleState.CREATED);
        
        // Scan for lifecycle callbacks
        scanLifecycleCallbacks(beanName, bean);
        
        LOGGER.fine("Registered bean for lifecycle: " + beanName);
    }
    
    /**
     * Scans a bean for lifecycle callback methods.
     */
    private void scanLifecycleCallbacks(String beanName, Object bean) {
        Class<?> clazz = bean.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            // @PostInitialize
            PostInitialize postInit = method.getAnnotation(PostInitialize.class);
            if (postInit != null) {
                postInitializeCallbacks.add(new LifecycleCallback(bean, beanName, method, postInit.order()));
            }
            
            // @OnStart
            OnStart onStart = method.getAnnotation(OnStart.class);
            if (onStart != null) {
                onStartCallbacks.add(new LifecycleCallback(bean, beanName, method, onStart.order()));
            }
            
            // @OnStop
            OnStop onStop = method.getAnnotation(OnStop.class);
            if (onStop != null) {
                onStopCallbacks.add(new LifecycleCallback(bean, beanName, method, onStop.order()));
            }
        }
    }
    
    /**
     * Applies post-processing to a bean before initialization.
     *
     * @param bean the bean instance
     * @param beanName the bean name
     * @return the processed bean (may be wrapped)
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : postProcessors) {
            Object processed = processor.postProcessBeforeInitialization(result, beanName);
            if (processed != null) {
                result = processed;
            }
        }
        return result;
    }
    
    /**
     * Applies post-processing to a bean after initialization.
     *
     * @param bean the bean instance
     * @param beanName the bean name
     * @return the processed bean (may be wrapped/proxied)
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : postProcessors) {
            Object processed = processor.postProcessAfterInitialization(result, beanName);
            if (processed != null) {
                result = processed;
            }
        }
        beanStates.put(beanName, BeanLifecycleState.POST_PROCESSED);
        return result;
    }
    
    /**
     * Initializes a bean implementing InitializingBean.
     *
     * @param bean the bean instance
     * @param beanName the bean name
     */
    public void initializeBean(Object bean, String beanName) {
        if (bean instanceof InitializingBean) {
            try {
                ((InitializingBean) bean).afterPropertiesSet();
                LOGGER.fine("Called afterPropertiesSet on: " + beanName);
            } catch (Exception e) {
                throw new RuntimeException("Error initializing bean: " + beanName, e);
            }
        }
        beanStates.put(beanName, BeanLifecycleState.INITIALIZED);
    }
    
    /**
     * Called when container refresh is complete.
     * Invokes all @PostInitialize methods and publishes ContextRefreshedEvent.
     *
     * @param initializationTimeMs time taken to initialize
     */
    public void onRefresh(long initializationTimeMs) {
        startTime = Instant.now();
        
        // Sort and invoke @PostInitialize callbacks
        Collections.sort(postInitializeCallbacks);
        for (LifecycleCallback callback : postInitializeCallbacks) {
            try {
                callback.invoke();
                LOGGER.fine("Invoked @PostInitialize on: " + callback.beanName);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in @PostInitialize: " + callback.beanName, e);
                throw new RuntimeException("@PostInitialize failed for: " + callback.beanName, e);
            }
        }
        
        // Publish event
        if (eventBus != null) {
            eventBus.publish(new ContextRefreshedEvent(this, managedBeans.size(), initializationTimeMs));
        }
        
        LOGGER.info("Container refreshed with " + managedBeans.size() + " beans in " + 
                initializationTimeMs + "ms");
    }
    
    /**
     * Starts all lifecycle beans.
     * Invokes Lifecycle.start(), @OnStart methods, and publishes ContextStartedEvent.
     */
    public void start() {
        if (running) {
            LOGGER.warning("Lifecycle already started");
            return;
        }
        
        int lifecycleCount = 0;
        
        // Start SmartLifecycle beans in phase order
        List<Map.Entry<String, Object>> smartLifecycleBeans = managedBeans.entrySet().stream()
                .filter(e -> e.getValue() instanceof SmartLifecycle)
                .filter(e -> ((SmartLifecycle) e.getValue()).isAutoStartup())
                .sorted(Comparator.comparingInt(e -> ((SmartLifecycle) e.getValue()).getPhase()))
                .collect(Collectors.toList());
        
        for (Map.Entry<String, Object> entry : smartLifecycleBeans) {
            SmartLifecycle lifecycle = (SmartLifecycle) entry.getValue();
            if (!lifecycle.isRunning()) {
                lifecycle.start();
                beanStates.put(entry.getKey(), BeanLifecycleState.STARTED);
                lifecycleCount++;
                LOGGER.fine("Started SmartLifecycle bean: " + entry.getKey() + 
                        " (phase=" + lifecycle.getPhase() + ")");
            }
        }
        
        // Start regular Lifecycle beans
        for (Map.Entry<String, Object> entry : managedBeans.entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof Lifecycle && !(bean instanceof SmartLifecycle)) {
                Lifecycle lifecycle = (Lifecycle) bean;
                if (!lifecycle.isRunning()) {
                    lifecycle.start();
                    beanStates.put(entry.getKey(), BeanLifecycleState.STARTED);
                    lifecycleCount++;
                    LOGGER.fine("Started Lifecycle bean: " + entry.getKey());
                }
            }
        }
        
        // Invoke @OnStart callbacks
        Collections.sort(onStartCallbacks);
        for (LifecycleCallback callback : onStartCallbacks) {
            try {
                callback.invoke();
                LOGGER.fine("Invoked @OnStart on: " + callback.beanName);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in @OnStart: " + callback.beanName, e);
                throw new RuntimeException("@OnStart failed for: " + callback.beanName, e);
            }
        }
        
        running = true;
        
        // Publish event
        if (eventBus != null) {
            eventBus.publish(new ContextStartedEvent(this, lifecycleCount));
        }
        
        LOGGER.info("Lifecycle started: " + lifecycleCount + " beans");
    }
    
    /**
     * Stops all lifecycle beans.
     * Invokes @OnStop methods, Lifecycle.stop(), and publishes ContextStoppedEvent.
     */
    public void stop() {
        if (!running) {
            LOGGER.warning("Lifecycle not running");
            return;
        }
        
        int lifecycleCount = 0;
        
        // Invoke @OnStop callbacks (reverse order)
        List<LifecycleCallback> reversedStopCallbacks = new ArrayList<>(onStopCallbacks);
        reversedStopCallbacks.sort(Comparator.comparingInt((LifecycleCallback c) -> c.order).reversed());
        
        for (LifecycleCallback callback : reversedStopCallbacks) {
            try {
                callback.invoke();
                LOGGER.fine("Invoked @OnStop on: " + callback.beanName);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in @OnStop: " + callback.beanName, e);
            }
        }
        
        // Stop SmartLifecycle beans in reverse phase order
        List<Map.Entry<String, Object>> smartLifecycleBeans = managedBeans.entrySet().stream()
                .filter(e -> e.getValue() instanceof SmartLifecycle)
                .sorted(Comparator.comparingInt(
                        (Map.Entry<String, Object> e) -> ((SmartLifecycle) e.getValue()).getPhase()
                ).reversed())
                .collect(Collectors.toList());
        
        for (Map.Entry<String, Object> entry : smartLifecycleBeans) {
            SmartLifecycle lifecycle = (SmartLifecycle) entry.getValue();
            if (lifecycle.isRunning()) {
                lifecycle.stop();
                beanStates.put(entry.getKey(), BeanLifecycleState.STOPPED);
                lifecycleCount++;
                LOGGER.fine("Stopped SmartLifecycle bean: " + entry.getKey());
            }
        }
        
        // Stop regular Lifecycle beans
        for (Map.Entry<String, Object> entry : managedBeans.entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof Lifecycle && !(bean instanceof SmartLifecycle)) {
                Lifecycle lifecycle = (Lifecycle) bean;
                if (lifecycle.isRunning()) {
                    lifecycle.stop();
                    beanStates.put(entry.getKey(), BeanLifecycleState.STOPPED);
                    lifecycleCount++;
                    LOGGER.fine("Stopped Lifecycle bean: " + entry.getKey());
                }
            }
        }
        
        running = false;
        
        // Publish event
        if (eventBus != null) {
            eventBus.publish(new ContextStoppedEvent(this, lifecycleCount));
        }
        
        LOGGER.info("Lifecycle stopped: " + lifecycleCount + " beans");
    }
    
    /**
     * Destroys all beans.
     * Invokes DisposableBean.destroy() and publishes ContextClosedEvent.
     */
    public void destroy() {
        if (running) {
            stop();
        }
        
        int destroyedCount = 0;
        
        // Destroy beans in reverse order
        List<String> beanNames = new ArrayList<>(managedBeans.keySet());
        Collections.reverse(beanNames);
        
        for (String beanName : beanNames) {
            Object bean = managedBeans.get(beanName);
            if (bean instanceof DisposableBean) {
                try {
                    ((DisposableBean) bean).destroy();
                    destroyedCount++;
                    LOGGER.fine("Destroyed bean: " + beanName);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error destroying bean: " + beanName, e);
                }
            }
            beanStates.put(beanName, BeanLifecycleState.DESTROYED);
        }
        
        Duration uptime = startTime != null ? Duration.between(startTime, Instant.now()) : Duration.ZERO;
        
        // Publish event
        if (eventBus != null) {
            eventBus.publish(new ContextClosedEvent(this, uptime, destroyedCount));
        }
        
        LOGGER.info("Container closed: " + destroyedCount + " beans destroyed, uptime: " + uptime);
        
        // Clear state
        managedBeans.clear();
        beanStates.clear();
        postInitializeCallbacks.clear();
        onStartCallbacks.clear();
        onStopCallbacks.clear();
    }
    
    /**
     * Returns whether the lifecycle is running.
     *
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Returns the number of registered BeanPostProcessors.
     *
     * @return post processor count
     */
    public int getPostProcessorCount() {
        return postProcessors.size();
    }
    
    /**
     * Returns the number of managed beans.
     *
     * @return bean count
     */
    public int getBeanCount() {
        return managedBeans.size();
    }
    
    /**
     * Returns statistics about the lifecycle processor.
     *
     * @return statistics string
     */
    public String getStatistics() {
        long startedCount = beanStates.values().stream()
                .filter(s -> s == BeanLifecycleState.STARTED)
                .count();
        
        return String.format("LifecycleProcessor{beans=%d, postProcessors=%d, " +
                        "postInitCallbacks=%d, onStartCallbacks=%d, onStopCallbacks=%d, " +
                        "running=%s, startedBeans=%d}",
                managedBeans.size(), postProcessors.size(),
                postInitializeCallbacks.size(), onStartCallbacks.size(), onStopCallbacks.size(),
                running, startedCount);
    }
}
