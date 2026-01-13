package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.runtime.ComponentFactory;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionScope implementation.
 */
@DisplayName("SessionScope Tests")
class SessionScopeTest {
    
    private SessionScope sessionScope;
    
    @BeforeEach
    void setUp() {
        sessionScope = new SessionScope();
        // Clear any previous state
        SessionScope.clearCurrentSession();
        SessionScope.clearSession("test-session");
        SessionScope.clearSession("another-session");
    }
    
    @AfterEach
    void tearDown() {
        SessionScope.clearCurrentSession();
        SessionScope.clearSession("test-session");
        SessionScope.clearSession("another-session");
    }
    
    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {
        
        @Test
        @DisplayName("Should have correct scope ID")
        void shouldHaveCorrectScopeId() {
            assertEquals("session", SessionScope.SCOPE_ID);
        }
        
        @Test
        @DisplayName("Should return 'session' as ID")
        void shouldReturnSessionAsId() {
            assertEquals("session", sessionScope.getId());
        }
        
        @Test
        @DisplayName("Should return 'Session' as display name")
        void shouldReturnSessionAsDisplayName() {
            assertEquals("Session", sessionScope.getDisplayName());
        }
        
        @Test
        @DisplayName("Should be inactive without context")
        void shouldBeInactiveWithoutContext() {
            assertFalse(sessionScope.isActive());
        }
    }
    
    @Nested
    @DisplayName("Session Context Tests")
    class SessionContextTests {
        
        @Test
        @DisplayName("Should throw exception when no session context")
        void shouldThrowExceptionWhenNoSessionContext() {
            ComponentFactory<String> factory = () -> "test-bean";
            
            assertThrows(SessionScope.NoSessionContextException.class, () -> {
                sessionScope.get("testBean", factory);
            });
        }
        
        @Test
        @DisplayName("Should detect when in session context")
        void shouldDetectWhenInSessionContext() {
            assertFalse(SessionScope.isInSessionContext());
            
            SessionScope.setCurrentSession("test-session");
            assertTrue(SessionScope.isInSessionContext());
            
            SessionScope.clearCurrentSession();
            assertFalse(SessionScope.isInSessionContext());
        }
    }
    
    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {
        
        @Test
        @DisplayName("Should create bean when session context is set")
        void shouldCreateBeanWhenSessionContextIsSet() {
            SessionScope.setCurrentSession("test-session");
            
            AtomicInteger factoryCallCount = new AtomicInteger(0);
            ComponentFactory<String> factory = () -> {
                factoryCallCount.incrementAndGet();
                return "session-bean";
            };
            
            String bean = sessionScope.get("testBean", factory);
            
            assertEquals("session-bean", bean);
            assertEquals(1, factoryCallCount.get());
        }
        
        @Test
        @DisplayName("Should return same instance on subsequent accesses within session")
        void shouldReturnSameInstanceOnSubsequentAccessesWithinSession() {
            SessionScope.setCurrentSession("test-session");
            
            ComponentFactory<String> factory = () -> "new-instance";
            
            String bean1 = sessionScope.get("testBean", factory);
            String bean2 = sessionScope.get("testBean", factory);
            
            assertSame(bean1, bean2);
        }
        
        @Test
        @DisplayName("Should create different instances for different sessions")
        void shouldCreateDifferentInstancesForDifferentSessions() {
            // Session 1
            SessionScope.setCurrentSession("test-session");
            String bean1 = sessionScope.get("bean", () -> "instance-1");
            
            // Clear and set Session 2
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            String bean2 = sessionScope.get("bean", () -> "instance-2");
            
            assertEquals("instance-1", bean1);
            assertEquals("instance-2", bean2);
            assertNotSame(bean1, bean2);
        }
        
        @Test
        @DisplayName("Should persist beans across session context switches")
        void shouldPersistBeansAcrossSessionContextSwitches() {
            // Set session 1 and create bean
            SessionScope.setCurrentSession("test-session");
            String bean1 = sessionScope.get("bean", () -> "session-1-bean");
            
            // Switch to session 2 and create bean
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            String bean2 = sessionScope.get("bean", () -> "session-2-bean");
            
            // Switch back to session 1
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("test-session");
            String bean1Again = sessionScope.get("bean", () -> "should-not-be-created");
            
            // Should get the original bean from session 1
            assertSame(bean1, bean1Again);
            assertNotSame(bean1, bean2);
        }
    }
    
    @Nested
    @DisplayName("Multiple Beans Tests")
    class MultipleBeansTests {
        
        @Test
        @DisplayName("Should create separate beans for different names")
        void shouldCreateSeparateBeansForDifferentNames() {
            SessionScope.setCurrentSession("test-session");
            
            ComponentFactory<String> factory1 = () -> "bean1";
            ComponentFactory<String> factory2 = () -> "bean2";
            ComponentFactory<String> factory3 = () -> "bean3";
            
            String bean1 = sessionScope.get("bean1", factory1);
            String bean2 = sessionScope.get("bean2", factory2);
            String bean3 = sessionScope.get("bean3", factory3);
            
            assertEquals("bean1", bean1);
            assertEquals("bean2", bean2);
            assertEquals("bean3", bean3);
            assertNotSame(bean1, bean2);
            assertNotSame(bean2, bean3);
            assertNotSame(bean3, bean1);
        }
        
        @Test
        @DisplayName("Should return correct bean count for session")
        void shouldReturnCorrectBeanCountForSession() {
            SessionScope.setCurrentSession("test-session");
            
            assertEquals(0, SessionScope.getSessionBeanCount());
            
            sessionScope.get("bean1", () -> "1");
            assertEquals(1, SessionScope.getSessionBeanCount());
            
            sessionScope.get("bean2", () -> "2");
            assertEquals(2, SessionScope.getSessionBeanCount());
            
            sessionScope.get("bean3", () -> "3");
            assertEquals(3, SessionScope.getSessionBeanCount());
        }
    }
    
    @Nested
    @DisplayName("Remove Tests")
    class RemoveTests {
        
        @Test
        @DisplayName("Should remove bean from current session")
        void shouldRemoveBeanFromCurrentSession() {
            SessionScope.setCurrentSession("test-session");
            
            ComponentFactory<String> factory = () -> "test-bean";
            
            String bean = sessionScope.get("testBean", factory);
            assertEquals("test-bean", bean);
            
            Object removed = sessionScope.remove("testBean");
            assertEquals("test-bean", removed);
        }
        
        @Test
        @DisplayName("Should return null for non-existent bean")
        void shouldReturnNullForNonExistentBean() {
            SessionScope.setCurrentSession("test-session");
            
            Object removed = sessionScope.remove("nonExistent");
            assertNull(removed);
        }
        
        @Test
        @DisplayName("Should not affect other sessions when removing")
        void shouldNotAffectOtherSessionsWhenRemoving() {
            // Create bean in session 1
            SessionScope.setCurrentSession("test-session");
            sessionScope.get("bean", () -> "session-1-bean");
            
            // Create bean in session 2
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            sessionScope.get("bean", () -> "session-2-bean");
            
            // Remove from session 2
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            sessionScope.remove("bean");
            
            // Verify session 1 bean still exists
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("test-session");
            String bean1 = sessionScope.get("bean", () -> "should-not-be-created");
            assertEquals("session-1-bean", bean1);
        }
    }
    
    @Nested
    @DisplayName("Clear Session Tests")
    class ClearSessionTests {
        
        @Test
        @DisplayName("Should clear all beans for specific session")
        void shouldClearAllBeansForSpecificSession() {
            // Create beans in session
            SessionScope.setCurrentSession("test-session");
            sessionScope.get("bean1", () -> "1");
            sessionScope.get("bean2", () -> "2");
            sessionScope.get("bean3", () -> "3");
            
            assertEquals(3, SessionScope.getSessionBeanCount());
            assertEquals(1, SessionScope.getActiveSessionCount());
            
            // Clear the session
            SessionScope.clearSession("test-session");
            
            assertEquals(0, SessionScope.getActiveSessionCount());
            
            // Verify new instance is created when accessing again
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("test-session");
            String newBean = sessionScope.get("bean1", () -> "new-instance");
            assertEquals("new-instance", newBean);
        }
        
        @Test
        @DisplayName("Should not affect other sessions when clearing")
        void shouldNotAffectOtherSessionsWhenClearing() {
            // Create beans in both sessions
            SessionScope.setCurrentSession("test-session");
            sessionScope.get("bean", () -> "session-1-bean");
            
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            sessionScope.get("bean", () -> "session-2-bean");
            
            // Clear session 1
            SessionScope.clearSession("test-session");
            
            // Session 2 should still have its bean
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            String bean2 = sessionScope.get("bean", () -> "should-not-be-created");
            assertEquals("session-2-bean", bean2);
        }
    }
    
    @Nested
    @DisplayName("Destroy Tests")
    class DestroyTests {
        
        @Test
        @DisplayName("Should clear all sessions on destroy")
        void shouldClearAllSessionsOnDestroy() {
            SessionScope.setCurrentSession("test-session");
            sessionScope.get("bean1", () -> "1");
            
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            sessionScope.get("bean2", () -> "2");
            
            assertEquals(2, SessionScope.getActiveSessionCount());
            
            sessionScope.destroy();
            
            assertEquals(0, SessionScope.getActiveSessionCount());
            assertFalse(sessionScope.isActive());
        }
    }
    
    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should handle concurrent access from same session")
        void shouldHandleConcurrentAccessFromSameSession() throws InterruptedException {
            SessionScope.setCurrentSession("test-session");
            
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger factoryCallCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        String bean = sessionScope.get("sharedBean", () -> {
                            factoryCallCount.incrementAndGet();
                            return "shared-instance";
                        });
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
            
            // Factory should be called only once (singleton within session)
            assertEquals(1, factoryCallCount.get());
            // All threads should get the same instance
            assertEquals(threadCount, successCount.get());
        }
        
        @Test
        @DisplayName("Should isolate different sessions in concurrent access")
        void shouldIsolateDifferentSessionsInConcurrentAccess() throws InterruptedException {
            String sessionId1 = "session-1";
            String sessionId2 = "session-2";
            
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            
            // Thread 1 - Session 1
            new Thread(() -> {
                SessionScope.setCurrentSession(sessionId1);
                String bean1 = sessionScope.get("bean", () -> sessionId1);
                if (sessionId1.equals(bean1)) {
                    successCount.incrementAndGet();
                }
                SessionScope.clearCurrentSession();
                latch.countDown();
            }).start();
            
            // Thread 2 - Session 2
            new Thread(() -> {
                SessionScope.setCurrentSession(sessionId2);
                String bean2 = sessionScope.get("bean", () -> sessionId2);
                if (sessionId2.equals(bean2)) {
                    successCount.incrementAndGet();
                }
                SessionScope.clearCurrentSession();
                latch.countDown();
            }).start();
            
            latch.await();
            
            // Each thread should get its own instance
            assertEquals(2, successCount.get());
        }
    }
    
    @Nested
    @DisplayName("Active Session Count Tests")
    class ActiveSessionCountTests {
        
        @Test
        @DisplayName("Should track active sessions count")
        void shouldTrackActiveSessionsCount() {
            assertEquals(0, SessionScope.getActiveSessionCount());
            
            SessionScope.setCurrentSession("test-session");
            sessionScope.get("bean", () -> "1");
            assertEquals(1, SessionScope.getActiveSessionCount());
            
            SessionScope.clearCurrentSession();
            SessionScope.setCurrentSession("another-session");
            sessionScope.get("bean", () -> "2");
            assertEquals(2, SessionScope.getActiveSessionCount());
        }
    }
    
    @Nested
    @DisplayName("Describe Tests")
    class DescribeTests {
        
        @Test
        @DisplayName("Should return accurate description when active")
        void shouldReturnAccurateDescriptionWhenActive() {
            SessionScope.setCurrentSession("test-session-12345678");
            
            sessionScope.get("bean1", () -> "1");
            
            String description = sessionScope.describe();
            
            assertTrue(description.contains("SessionScope"));
            assertTrue(description.contains("session=test-..."));
            assertTrue(description.contains("beans=1"));
        }
        
        @Test
        @DisplayName("Should show inactive when no context")
        void shouldShowInactiveWhenNoContext() {
            String description = sessionScope.describe();
            
            assertEquals("SessionScope[inactive]", description);
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle null session ID in setCurrentSession")
        void shouldHandleNullSessionIdInSetCurrentSession() {
            SessionScope.setCurrentSession(null);
            
            // Should not throw, but context should be inactive
            assertFalse(sessionScope.isActive());
        }
        
        @Test
        @DisplayName("Should handle clearing non-existent session")
        void shouldHandleClearingNonExistentSession() {
            // Should not throw
            Map<String, Object> cleared = SessionScope.clearSession("non-existent-session");
            assertNull(cleared);
        }
        
        @Test
        @DisplayName("Should handle empty bean name")
        void shouldHandleEmptyBeanName() {
            SessionScope.setCurrentSession("test-session");
            
            String bean = sessionScope.get("", () -> "empty-name-bean");
            
            assertEquals("empty-name-bean", bean);
        }
        
        @Test
        @DisplayName("Should handle special characters in bean name")
        void shouldHandleSpecialCharactersInBeanName() {
            SessionScope.setCurrentSession("test-session");
            
            String bean = sessionScope.get("bean-with-special-chars_123.test", () -> "special");
            
            assertEquals("special", bean);
        }
        
        @Test
        @DisplayName("Should handle long session ID")
        void shouldHandleLongSessionId() {
            String longSessionId = "very-long-session-id-that-might-cause-issues-in-description";
            SessionScope.setCurrentSession(longSessionId);
            
            ComponentFactory<String> factory = () -> "bean";
            String bean = sessionScope.get("testBean", factory);
            
            assertEquals("bean", bean);
        }
    }
}
