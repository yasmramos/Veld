package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.annotation.ScopeType;
import io.github.yasmramos.veld.runtime.ComponentFactory;
import org.junit.jupiter.api.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RequestScope implementation.
 */
@DisplayName("RequestScope Tests")
class RequestScopeTest {
    
    private RequestScope requestScope;
    
    @BeforeEach
    void setUp() {
        requestScope = new RequestScope();
        // Destroy all previous state to ensure clean test environment
        requestScope.destroy();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up all request state
        requestScope.destroy();
    }
    
    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {
        
        @Test
        @DisplayName("Should have correct scope ID")
        void shouldHaveCorrectScopeId() {
            assertEquals("request", RequestScope.SCOPE_ID);
        }
        
        @Test
        @DisplayName("Should return 'request' as ID")
        void shouldReturnRequestAsId() {
            assertEquals("request", requestScope.getId());
        }
        
        @Test
        @DisplayName("Should return 'Request' as display name")
        void shouldReturnRequestAsDisplayName() {
            assertEquals("Request", requestScope.getDisplayName());
        }
        
        @Test
        @DisplayName("Should be inactive without context")
        void shouldBeInactiveWithoutContext() {
            assertFalse(requestScope.isActive());
        }
    }
    
    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {
        
        @Test
        @DisplayName("Should create bean on first access")
        void shouldCreateBeanOnFirstAccess() {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            AtomicInteger factoryCallCount = new AtomicInteger(0);
            ComponentFactory<String> factory = createFactory("test-bean", factoryCallCount);
            
            String bean = requestScope.get("testBean", factory);
            
            assertEquals("test-bean", bean);
            assertEquals(1, factoryCallCount.get());
        }
        
        @Test
        @DisplayName("Should return same instance on subsequent accesses")
        void shouldReturnSameInstanceOnSubsequentAccesses() {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            ComponentFactory<String> factory = createFactory("new-instance", null);
            
            String bean1 = requestScope.get("testBean", factory);
            String bean2 = requestScope.get("testBean", factory);
            
            assertSame(bean1, bean2);
        }
        
        @Test
        @DisplayName("Should create separate beans for different names")
        void shouldCreateSeparateBeansForDifferentNames() {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            ComponentFactory<String> factory1 = createFactory("bean1", null);
            ComponentFactory<String> factory2 = createFactory("bean2", null);
            
            String bean1 = requestScope.get("bean1", factory1);
            String bean2 = requestScope.get("bean2", factory2);
            
            assertEquals("bean1", bean1);
            assertEquals("bean2", bean2);
            assertNotSame(bean1, bean2);
        }
        
        @Test
        @DisplayName("Should create different instances in different requests")
        void shouldCreateDifferentInstancesInDifferentRequests() {
            // First request
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            String bean1 = requestScope.get("bean", createFactory("instance-1", null));
            
            // Clear and start second request
            RequestScope.clearRequestScope();
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            String bean2 = requestScope.get("bean", createFactory("instance-2", null));
            
            assertEquals("instance-1", bean1);
            assertEquals("instance-2", bean2);
            assertNotSame(bean1, bean2);
        }
    }
    
    @Nested
    @DisplayName("Remove Tests")
    class RemoveTests {
        
        @Test
        @DisplayName("Should remove bean from scope")
        void shouldRemoveBeanFromScope() {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            ComponentFactory<String> factory = createFactory("test-bean", null);
            
            String bean = requestScope.get("testBean", factory);
            assertEquals("test-bean", bean);
            
            Object removed = requestScope.remove("testBean");
            assertEquals("test-bean", removed);
            
            // Verify new instance is created on next access
            String newBean = requestScope.get("testBean", factory);
            assertNotSame(bean, newBean);
        }
        
        @Test
        @DisplayName("Should return null for non-existent bean")
        void shouldReturnNullForNonExistentBean() {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            Object removed = requestScope.remove("nonExistent");
            assertNull(removed);
        }
    }
    
    @Nested
    @DisplayName("Destroy Tests")
    class DestroyTests {
        
        @Test
        @DisplayName("Should clear all beans on destroy")
        void shouldClearAllBeansOnDestroy() {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            requestScope.get("bean1", createFactory("1", null));
            requestScope.get("bean2", createFactory("2", null));
            
            assertEquals(2, RequestScope.getRequestBeanCount());
            
            requestScope.destroy();
            
            assertEquals(0, RequestScope.getRequestBeanCount());
            assertFalse(requestScope.isActive());
        }
    }
    
    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should handle concurrent access from same request")
        void shouldHandleConcurrentAccessFromSameRequest() throws InterruptedException {
            // Set up request context
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger factoryCallCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        ComponentFactory<String> factory = createFactory("shared-instance", factoryCallCount);
                        String bean = requestScope.get("sharedBean", factory);
                        if ("shared-instance".equals(bean)) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Factory should be called only once (singleton within request)
            assertEquals(1, factoryCallCount.get());
            // All threads should get the same instance
            assertEquals(threadCount, successCount.get());
        }
        
        @Test
        @DisplayName("Should isolate beans between different threads")
        void shouldIsolateBeansBetweenDifferentThreads() throws InterruptedException {
            String sessionId1 = "session-1";
            String sessionId2 = "session-2";
            
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            
            // Thread 1 - Session 1
            new Thread(() -> {
                RequestScope.setRequestScope(new ConcurrentHashMap<>());
                ComponentFactory<String> factory1 = createFactory(sessionId1, null);
                String bean1 = requestScope.get("bean", factory1);
                if (sessionId1.equals(bean1)) {
                    successCount.incrementAndGet();
                }
                RequestScope.clearRequestScope();
                latch.countDown();
            }).start();
            
            // Thread 2 - Session 2
            new Thread(() -> {
                RequestScope.setRequestScope(new ConcurrentHashMap<>());
                ComponentFactory<String> factory2 = createFactory(sessionId2, null);
                String bean2 = requestScope.get("bean", factory2);
                if (sessionId2.equals(bean2)) {
                    successCount.incrementAndGet();
                }
                RequestScope.clearRequestScope();
                latch.countDown();
            }).start();
            
            latch.await();
            
            // Each thread should get its own instance
            assertEquals(2, successCount.get());
        }
    }
    
    @Nested
    @DisplayName("Context Management Tests")
    class ContextManagementTests {
        
        @Test
        @DisplayName("Should detect when in request context")
        void shouldDetectWhenInRequestContext() {
            assertFalse(RequestScope.isInRequestContext());
            
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            assertTrue(RequestScope.isInRequestContext());
            
            RequestScope.clearRequestScope();
            assertFalse(RequestScope.isInRequestContext());
        }
        
        @Test
        @DisplayName("Should return bean count for current request")
        void shouldReturnBeanCountForCurrentRequest() {
            assertEquals(0, RequestScope.getRequestBeanCount());
            
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            requestScope.get("bean1", createFactory("1", null));
            assertEquals(1, RequestScope.getRequestBeanCount());
            
            requestScope.get("bean2", createFactory("2", null));
            assertEquals(2, RequestScope.getRequestBeanCount());
        }
        
        @Test
        @DisplayName("Should return 0 when not in request context")
        void shouldReturnZeroWhenNotInRequestContext() {
            assertEquals(0, RequestScope.getRequestBeanCount());
        }
    }
    
    @Nested
    @DisplayName("Describe Tests")
    class DescribeTests {
        
        @Test
        @DisplayName("Should return accurate description")
        void shouldReturnAccurateDescription() {
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            requestScope.get("bean1", createFactory("1", null));
            
            String description = requestScope.describe();
            
            assertTrue(description.contains("RequestScope"));
            assertTrue(description.contains("beans=1"));
            assertTrue(description.contains("active=true"));
        }
        
        @Test
        @DisplayName("Should show inactive when no context")
        void shouldShowInactiveWhenNoContext() {
            String description = requestScope.describe();
            
            assertTrue(description.contains("active=false"));
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle null factory gracefully")
        void shouldHandleNullFactory() {
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            // This will throw NullPointerException as expected
            assertThrows(NullPointerException.class, () -> {
                requestScope.get("bean", null);
            });
        }
        
        @Test
        @DisplayName("Should handle empty bean name")
        void shouldHandleEmptyBeanName() {
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            String bean = requestScope.get("", createFactory("empty-name-bean", null));
            
            assertEquals("empty-name-bean", bean);
        }
        
        @Test
        @DisplayName("Should handle special characters in bean name")
        void shouldHandleSpecialCharactersInBeanName() {
            RequestScope.setRequestScope(new ConcurrentHashMap<>());
            
            String bean = requestScope.get("bean-with-special-chars_123.test", createFactory("special", null));
            
            assertEquals("special", bean);
        }
    }
    
    /**
     * Helper method to create a ComponentFactory for testing.
     */
    private <T> ComponentFactory<T> createFactory(T instance, AtomicInteger callCount) {
        return new ComponentFactory<T>() {
            private final T value = instance;
            private final AtomicInteger count = callCount;
            
            @Override
            public T create() {
                if (count != null) {
                    count.incrementAndGet();
                }
                return value;
            }
            
            @Override
            public Class<T> getComponentType() {
                @SuppressWarnings("unchecked")
                Class<T> type = (Class<T>) value.getClass();
                return type;
            }
            
            @Override
            public String getComponentName() {
                return "test-component";
            }
            
            @Override
            public ScopeType getScope() {
                return ScopeType.REQUEST;
            }
            
            @Override
            public void invokePostConstruct(T instance) {
                // No-op for testing
            }
            
            @Override
            public void invokePreDestroy(T instance) {
                // No-op for testing
            }
        };
    }
}
