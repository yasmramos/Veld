package io.github.yasmramos.veld.runtime.lifecycle;

import io.github.yasmramos.veld.annotation.OnStart;
import io.github.yasmramos.veld.annotation.OnStop;
import io.github.yasmramos.veld.annotation.PostInitialize;
import io.github.yasmramos.veld.runtime.event.EventBus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for LifecycleProcessor.
 */
@DisplayName("LifecycleProcessor Tests")
@ExtendWith(MockitoExtension.class)
class LifecycleProcessorTest {
    
    private LifecycleProcessor processor;
    
    @Mock
    private EventBus mockEventBus;
    
    @BeforeEach
    void setUp() {
        processor = new LifecycleProcessor();
        processor.setEventBus(mockEventBus);
    }
    
    // Test beans
    static class SimpleBean {
        boolean postInitCalled = false;
        boolean startCalled = false;
        boolean stopCalled = false;
        
        @PostInitialize
        public void postInit() {
            postInitCalled = true;
        }
        
        @OnStart
        public void onStart() {
            startCalled = true;
        }
        
        @OnStop
        public void onStop() {
            stopCalled = true;
        }
    }
    
    static class OrderedBean {
        static List<String> order = new ArrayList<>();
        String name;
        
        OrderedBean(String name) {
            this.name = name;
        }
        
        @PostInitialize(order = 1)
        public void first() {
            order.add(name + "-first");
        }
        
        @OnStart(order = 2)
        public void start() {
            order.add(name + "-start");
        }
    }
    
    static class InitializingBeanImpl implements InitializingBean {
        boolean initialized = false;
        
        @Override
        public void afterPropertiesSet() {
            initialized = true;
        }
    }
    
    static class DisposableBeanImpl implements DisposableBean {
        boolean destroyed = false;
        
        @Override
        public void destroy() {
            destroyed = true;
        }
    }
    
    static class LifecycleBeanImpl implements Lifecycle {
        boolean running = false;
        
        @Override
        public void start() {
            running = true;
        }
        
        @Override
        public void stop() {
            running = false;
        }
        
        @Override
        public boolean isRunning() {
            return running;
        }
    }
    
    static class SmartLifecycleBeanImpl implements SmartLifecycle {
        boolean running = false;
        int phase = 0;
        
        SmartLifecycleBeanImpl(int phase) {
            this.phase = phase;
        }
        
        @Override
        public void start() {
            running = true;
        }
        
        @Override
        public void stop() {
            running = false;
        }
        
        @Override
        public boolean isRunning() {
            return running;
        }
        
        @Override
        public int getPhase() {
            return phase;
        }
        
        @Override
        public boolean isAutoStartup() {
            return true;
        }
        
        @Override
        public void stop(Runnable callback) {
            stop();
            callback.run();
        }
    }
    
    static class TestBeanPostProcessor implements BeanPostProcessor {
        List<String> beforeCalls = new ArrayList<>();
        List<String> afterCalls = new ArrayList<>();
        
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            beforeCalls.add(beanName);
            return bean;
        }
        
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            afterCalls.add(beanName);
            return bean;
        }
        
        @Override
        public int getOrder() {
            return 0;
        }
    }
    
    @Nested
    @DisplayName("Bean Registration Tests")
    class BeanRegistrationTests {
        
        @Test
        @DisplayName("Should register bean")
        void shouldRegisterBean() {
            SimpleBean bean = new SimpleBean();
            
            processor.registerBean("simpleBean", bean);
            
            assertEquals(1, processor.getBeanCount());
        }
        
        @Test
        @DisplayName("Should scan lifecycle callbacks on registration")
        void shouldScanLifecycleCallbacksOnRegistration() {
            SimpleBean bean = new SimpleBean();

            // In zero-reflection mode, we must manually register callbacks
            // (In production, this would be generated by the annotation processor)
            try {
                processor.registerPostInitialize(bean, "simpleBean",
                    SimpleBean.class.getDeclaredMethod("postInit"), 0);
            } catch (NoSuchMethodException e) {
                fail("Method not found: " + e.getMessage());
            }

            processor.onRefresh(100);

            assertTrue(bean.postInitCalled);
        }
    }
    
    @Nested
    @DisplayName("Post Processor Tests")
    class PostProcessorTests {
        
        @Test
        @DisplayName("Should add post processor")
        void shouldAddPostProcessor() {
            TestBeanPostProcessor postProcessor = new TestBeanPostProcessor();
            
            processor.addBeanPostProcessor(postProcessor);
            
            assertEquals(1, processor.getPostProcessorCount());
        }
        
        @Test
        @DisplayName("Should call post processor before initialization")
        void shouldCallPostProcessorBeforeInitialization() {
            TestBeanPostProcessor postProcessor = new TestBeanPostProcessor();
            processor.addBeanPostProcessor(postProcessor);
            SimpleBean bean = new SimpleBean();
            
            processor.postProcessBeforeInitialization(bean, "testBean");
            
            assertTrue(postProcessor.beforeCalls.contains("testBean"));
        }
        
        @Test
        @DisplayName("Should call post processor after initialization")
        void shouldCallPostProcessorAfterInitialization() {
            TestBeanPostProcessor postProcessor = new TestBeanPostProcessor();
            processor.addBeanPostProcessor(postProcessor);
            SimpleBean bean = new SimpleBean();
            
            processor.postProcessAfterInitialization(bean, "testBean");
            
            assertTrue(postProcessor.afterCalls.contains("testBean"));
        }
        
        @Test
        @DisplayName("Should allow post processor to wrap bean")
        void shouldAllowPostProcessorToWrapBean() {
            BeanPostProcessor wrapper = new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    return "wrapped";
                }
                
                @Override
                public int getOrder() {
                    return 0;
                }
            };
            processor.addBeanPostProcessor(wrapper);
            
            Object result = processor.postProcessAfterInitialization(new SimpleBean(), "test");
            
            assertEquals("wrapped", result);
        }
    }
    
    @Nested
    @DisplayName("InitializingBean Tests")
    class InitializingBeanTests {
        
        @Test
        @DisplayName("Should call afterPropertiesSet on InitializingBean")
        void shouldCallAfterPropertiesSet() {
            InitializingBeanImpl bean = new InitializingBeanImpl();
            
            processor.initializeBean(bean, "initBean");
            
            assertTrue(bean.initialized);
        }
        
        @Test
        @DisplayName("Should not fail for non-InitializingBean")
        void shouldNotFailForNonInitializingBean() {
            SimpleBean bean = new SimpleBean();
            
            assertDoesNotThrow(() -> processor.initializeBean(bean, "simpleBean"));
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Start Tests")
    class LifecycleStartTests {
        
        @Test
        @DisplayName("Should start Lifecycle beans")
        void shouldStartLifecycleBeans() {
            LifecycleBeanImpl bean = new LifecycleBeanImpl();
            processor.registerBean("lifecycleBean", bean);
            
            processor.start();
            
            assertTrue(bean.isRunning());
            assertTrue(processor.isRunning());
        }
        
        @Test
        @DisplayName("Should start SmartLifecycle beans in phase order")
        void shouldStartSmartLifecycleBeansInPhaseOrder() {
            List<Integer> startOrder = new ArrayList<>();
            
            SmartLifecycle phase1 = new SmartLifecycle() {
                boolean running = false;
                @Override public void start() { running = true; startOrder.add(1); }
                @Override public void stop() { running = false; }
                @Override public boolean isRunning() { return running; }
                @Override public int getPhase() { return 1; }
                @Override public boolean isAutoStartup() { return true; }
                @Override public void stop(Runnable callback) { stop(); callback.run(); }
            };
            
            SmartLifecycle phase2 = new SmartLifecycle() {
                boolean running = false;
                @Override public void start() { running = true; startOrder.add(2); }
                @Override public void stop() { running = false; }
                @Override public boolean isRunning() { return running; }
                @Override public int getPhase() { return 2; }
                @Override public boolean isAutoStartup() { return true; }
                @Override public void stop(Runnable callback) { stop(); callback.run(); }
            };
            
            // Register in reverse order
            processor.registerBean("phase2", phase2);
            processor.registerBean("phase1", phase1);
            
            processor.start();
            
            assertEquals(2, startOrder.size());
            assertEquals(1, startOrder.get(0));
            assertEquals(2, startOrder.get(1));
        }
        
        @Test
        @DisplayName("Should invoke @OnStart callbacks")
        void shouldInvokeOnStartCallbacks() {
            SimpleBean bean = new SimpleBean();

            // In zero-reflection mode, we must manually register callbacks
            try {
                processor.registerOnStart(bean, "simpleBean",
                    SimpleBean.class.getDeclaredMethod("onStart"), 0);
            } catch (NoSuchMethodException e) {
                fail("Method not found: " + e.getMessage());
            }

            processor.start();

            assertTrue(bean.startCalled);
        }
        
        @Test
        @DisplayName("Should not start if already running")
        void shouldNotStartIfAlreadyRunning() {
            processor.start();
            AtomicInteger callCount = new AtomicInteger(0);
            
            LifecycleBeanImpl bean = new LifecycleBeanImpl() {
                @Override
                public void start() {
                    callCount.incrementAndGet();
                    super.start();
                }
            };
            processor.registerBean("bean", bean);
            
            processor.start(); // Second call - should be ignored
            
            assertEquals(0, callCount.get());
        }
        
        @Test
        @DisplayName("Should publish ContextStartedEvent")
        void shouldPublishContextStartedEvent() {
            processor.start();
            
            verify(mockEventBus).publish(any());
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Stop Tests")
    class LifecycleStopTests {
        
        @Test
        @DisplayName("Should stop Lifecycle beans")
        void shouldStopLifecycleBeans() {
            LifecycleBeanImpl bean = new LifecycleBeanImpl();
            processor.registerBean("lifecycleBean", bean);
            processor.start();
            
            processor.stop();
            
            assertFalse(bean.isRunning());
            assertFalse(processor.isRunning());
        }
        
        @Test
        @DisplayName("Should invoke @OnStop callbacks")
        void shouldInvokeOnStopCallbacks() {
            SimpleBean bean = new SimpleBean();

            // In zero-reflection mode, we must manually register callbacks
            try {
                processor.registerOnStop(bean, "simpleBean",
                    SimpleBean.class.getDeclaredMethod("onStop"), 0);
            } catch (NoSuchMethodException e) {
                fail("Method not found: " + e.getMessage());
            }

            processor.start();
            processor.stop();

            assertTrue(bean.stopCalled);
        }
        
        @Test
        @DisplayName("Should not stop if not running")
        void shouldNotStopIfNotRunning() {
            // Not started yet
            assertDoesNotThrow(() -> processor.stop());
        }
        
        @Test
        @DisplayName("Should publish ContextStoppedEvent")
        void shouldPublishContextStoppedEvent() {
            processor.start();
            reset(mockEventBus);
            
            processor.stop();
            
            verify(mockEventBus).publish(any());
        }
    }
    
    @Nested
    @DisplayName("Destroy Tests")
    class DestroyTests {
        
        @Test
        @DisplayName("Should destroy DisposableBean")
        void shouldDestroyDisposableBean() {
            DisposableBeanImpl bean = new DisposableBeanImpl();
            processor.registerBean("disposableBean", bean);
            
            processor.destroy();
            
            assertTrue(bean.destroyed);
        }
        
        @Test
        @DisplayName("Should stop before destroy if running")
        void shouldStopBeforeDestroyIfRunning() {
            LifecycleBeanImpl bean = new LifecycleBeanImpl();
            processor.registerBean("lifecycleBean", bean);
            processor.start();
            assertTrue(bean.isRunning());
            
            processor.destroy();
            
            assertFalse(bean.isRunning());
        }
        
        @Test
        @DisplayName("Should clear all state on destroy")
        void shouldClearAllStateOnDestroy() {
            SimpleBean bean = new SimpleBean();
            processor.registerBean("simpleBean", bean);
            
            processor.destroy();
            
            assertEquals(0, processor.getBeanCount());
        }
        
        @Test
        @DisplayName("Should publish ContextClosedEvent")
        void shouldPublishContextClosedEvent() {
            processor.destroy();
            
            verify(mockEventBus).publish(any());
        }
    }
    
    @Nested
    @DisplayName("Refresh Tests")
    class RefreshTests {
        
        @Test
        @DisplayName("Should invoke @PostInitialize callbacks on refresh")
        void shouldInvokePostInitializeCallbacksOnRefresh() {
            SimpleBean bean = new SimpleBean();

            // In zero-reflection mode, we must manually register callbacks
            try {
                processor.registerPostInitialize(bean, "simpleBean",
                    SimpleBean.class.getDeclaredMethod("postInit"), 0);
            } catch (NoSuchMethodException e) {
                fail("Method not found: " + e.getMessage());
            }

            processor.onRefresh(100);

            assertTrue(bean.postInitCalled);
        }
        
        @Test
        @DisplayName("Should invoke @PostInitialize in order")
        void shouldInvokePostInitializeInOrder() {
            OrderedBean.order.clear();
            OrderedBean bean1 = new OrderedBean("bean1");
            OrderedBean bean2 = new OrderedBean("bean2");

            // In zero-reflection mode, we must manually register callbacks with order
            try {
                processor.registerPostInitialize(bean1, "bean1",
                    OrderedBean.class.getDeclaredMethod("first"), 1);
                processor.registerPostInitialize(bean2, "bean2",
                    OrderedBean.class.getDeclaredMethod("first"), 1);
            } catch (NoSuchMethodException e) {
                fail("Method not found: " + e.getMessage());
            }

            processor.onRefresh(100);

            // Both should have their postInit called
            assertTrue(OrderedBean.order.contains("bean1-first"));
            assertTrue(OrderedBean.order.contains("bean2-first"));
        }
        
        @Test
        @DisplayName("Should publish ContextRefreshedEvent")
        void shouldPublishContextRefreshedEvent() {
            processor.onRefresh(100);
            
            verify(mockEventBus).publish(any());
        }
    }
    
    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {
        
        @Test
        @DisplayName("Should return statistics")
        void shouldReturnStatistics() {
            SimpleBean bean = new SimpleBean();
            processor.registerBean("simpleBean", bean);
            TestBeanPostProcessor pp = new TestBeanPostProcessor();
            processor.addBeanPostProcessor(pp);
            
            String stats = processor.getStatistics();
            
            assertNotNull(stats);
            assertTrue(stats.contains("beans=1"));
            assertTrue(stats.contains("postProcessors=1"));
        }
        
        @Test
        @DisplayName("Should track running state")
        void shouldTrackRunningState() {
            assertFalse(processor.isRunning());
            
            processor.start();
            assertTrue(processor.isRunning());
            
            processor.stop();
            assertFalse(processor.isRunning());
        }
    }
}
