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

import io.github.yasmramos.veld.annotation.OnStart;
import io.github.yasmramos.veld.annotation.OnStop;
import io.github.yasmramos.veld.annotation.PostInitialize;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.lifecycle.event.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 * <p>
 * This processor is designed for zero-reflection mode, using pre-computed
 * MethodHandles for fast invocation without reflection overhead.
 * <p>
 * All lifecycle callbacks must be registered via the manual registration methods:
 * <ul>
 *   <li>{@link #registerPostInitialize(Object, String, MethodHandle)}</li>
 *   <li>{@link #registerOnStart(Object, String, MethodHandle)}</li>
 *   <li>{@link #registerOnStop(Object, String, MethodHandle)}</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 */
public class LifecycleProcessor {

    private static final Logger LOGGER = Logger.getLogger(LifecycleProcessor.class.getName());
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

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
     * Represents a lifecycle callback method with pre-computed MethodHandle.
     * <p>
     * This class replaces reflection-based invocation with MethodHandle for
     * zero-reflection compatibility with GraalVM Native Image.
     */
    private static class LifecycleCallback implements Comparable<LifecycleCallback> {
        final Object bean;
        final String beanName;
        final MethodHandle methodHandle;
        final int order;

        LifecycleCallback(Object bean, String beanName, MethodHandle methodHandle, int order) {
            this.bean = bean;
            this.beanName = beanName;
            this.methodHandle = methodHandle;
            this.order = order;
        }

        void invoke() throws Throwable {
            methodHandle.invoke();
        }

        @Override
        public int compareTo(LifecycleCallback other) {
            return Integer.compare(this.order, other.order);
        }
    }

    public LifecycleProcessor() {
    }

    /**
     * Registers a PostInitialize callback (zero-reflection mode).
     *
     * @param bean        the bean instance
     * @param beanName    the bean name
     * @param methodHandle pre-computed MethodHandle for the callback method
     * @param order       the execution order
     */
    public void registerPostInitialize(Object bean, String beanName, MethodHandle methodHandle, int order) {
        postInitializeCallbacks.add(new LifecycleCallback(bean, beanName, methodHandle, order));
    }

    /**
     * Registers an OnStart callback (zero-reflection mode).
     *
     * @param bean        the bean instance
     * @param beanName    the bean name
     * @param methodHandle pre-computed MethodHandle for the callback method
     * @param order       the execution order
     */
    public void registerOnStart(Object bean, String beanName, MethodHandle methodHandle, int order) {
        onStartCallbacks.add(new LifecycleCallback(bean, beanName, methodHandle, order));
    }

    /**
     * Registers an OnStop callback (zero-reflection mode).
     *
     * @param bean        the bean instance
     * @param beanName    the bean name
     * @param methodHandle pre-computed MethodHandle for the callback method
     * @param order       the execution order
     */
    public void registerOnStop(Object bean, String beanName, MethodHandle methodHandle, int order) {
        onStopCallbacks.add(new LifecycleCallback(bean, beanName, methodHandle, order));
    }

    /**
     * Creates a MethodHandle for a no-argument method.
     * <p>
     * This method is used by code generators to create MethodHandles
     * for lifecycle callbacks, eliminating the need for reflection.
     *
     * @param target the bean instance
     * @param methodName the name of the method to invoke
     * @return a MethodHandle for the method
     * @throws NoSuchMethodException if the method doesn't exist
     * @throws IllegalAccessException if the method is not accessible
     */
    public static MethodHandle createMethodHandle(Object target, String methodName)
            throws NoSuchMethodException, IllegalAccessException {
        return LOOKUP.findVirtual(
                target.getClass(),
                methodName,
                MethodType.methodType(void.class)
        ).bindTo(target);
    }

    /**
     * Creates a MethodHandle for a method with one parameter.
     *
     * @param target      the bean instance
     * @param methodName  the name of the method to invoke
     * @param paramClass  the class of the single parameter
     * @return a MethodHandle for the method
     * @throws NoSuchMethodException if the method doesn't exist
     * @throws IllegalAccessException if the method is not accessible
     */
    public static MethodHandle createMethodHandle(Object target, String methodName, Class<?> paramClass)
            throws NoSuchMethodException, IllegalAccessException {
        return LOOKUP.findVirtual(
                target.getClass(),
                methodName,
                MethodType.methodType(void.class, paramClass)
        ).bindTo(target);
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void addBeanPostProcessor(BeanPostProcessor postProcessor) {
        postProcessors.add(postProcessor);
        postProcessors.sort(Comparator.comparingInt(BeanPostProcessor::getOrder));
        LOGGER.fine("Registered BeanPostProcessor: " + postProcessor.getClass().getSimpleName());
    }

    public void registerBean(String beanName, Object bean) {
        managedBeans.put(beanName, bean);
        beanStates.put(beanName, BeanLifecycleState.CREATED);
        LOGGER.fine("Registered bean for lifecycle: " + beanName);
    }

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

    public void onRefresh(long initializationTimeMs) {
        startTime = Instant.now();

        Collections.sort(postInitializeCallbacks);
        for (LifecycleCallback callback : postInitializeCallbacks) {
            try {
                callback.invoke();
                LOGGER.fine("Invoked @PostInitialize on: " + callback.beanName);
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Error in @PostInitialize: " + callback.beanName, e);
                throw new RuntimeException("@PostInitialize failed for: " + callback.beanName, e);
            }
        }

        if (eventBus != null) {
            eventBus.publish(new ContextRefreshedEvent(this, managedBeans.size(), initializationTimeMs));
        }

        LOGGER.info("Container refreshed with " + managedBeans.size() + " beans in " +
                initializationTimeMs + "ms");
    }

    public void start() {
        if (running) {
            LOGGER.warning("Lifecycle already started");
            return;
        }

        int lifecycleCount = 0;

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

        Collections.sort(onStartCallbacks);
        for (LifecycleCallback callback : onStartCallbacks) {
            try {
                callback.invoke();
                LOGGER.fine("Invoked @OnStart on: " + callback.beanName);
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Error in @OnStart: " + callback.beanName, e);
                throw new RuntimeException("@OnStart failed for: " + callback.beanName, e);
            }
        }

        running = true;

        if (eventBus != null) {
            eventBus.publish(new ContextStartedEvent(this, lifecycleCount));
        }

        LOGGER.info("Lifecycle started: " + lifecycleCount + " beans");
    }

    public void stop() {
        if (!running) {
            LOGGER.warning("Lifecycle not running");
            return;
        }

        int lifecycleCount = 0;

        List<LifecycleCallback> reversedStopCallbacks = new ArrayList<>(onStopCallbacks);
        reversedStopCallbacks.sort(Comparator.comparingInt((LifecycleCallback c) -> c.order).reversed());

        for (LifecycleCallback callback : reversedStopCallbacks) {
            try {
                callback.invoke();
                LOGGER.fine("Invoked @OnStop on: " + callback.beanName);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Error in @OnStop: " + callback.beanName, e);
            }
        }

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

        if (eventBus != null) {
            eventBus.publish(new ContextStoppedEvent(this, lifecycleCount));
        }

        LOGGER.info("Lifecycle stopped: " + lifecycleCount + " beans");
    }

    public void destroy() {
        if (running) {
            stop();
        }

        int destroyedCount = 0;

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

        if (eventBus != null) {
            eventBus.publish(new ContextClosedEvent(this, uptime, destroyedCount));
        }

        LOGGER.info("Container closed: " + destroyedCount + " beans destroyed, uptime: " + uptime);

        managedBeans.clear();
        beanStates.clear();
        postInitializeCallbacks.clear();
        onStartCallbacks.clear();
        onStopCallbacks.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public int getPostProcessorCount() {
        return postProcessors.size();
    }

    public int getBeanCount() {
        return managedBeans.size();
    }

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
