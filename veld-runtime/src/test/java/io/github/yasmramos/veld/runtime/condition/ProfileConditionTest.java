package io.github.yasmramos.veld.runtime.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProfileCondition class.
 */
@DisplayName("ProfileCondition Tests")
class ProfileConditionTest {

    private ConditionContext createContextWithProfiles(String... profiles) {
        Set<String> profileSet = new HashSet<>(Arrays.asList(profiles));
        return new ConditionContext(null, profileSet);
    }

    @Nested
    @DisplayName("Simple Profile Matching")
    class SimpleProfileMatchingTests {

        @Test
        @DisplayName("Should match when profile is active")
        void shouldMatchWhenProfileIsActive() {
            ProfileCondition condition = new ProfileCondition("dev");
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when profile is inactive")
        void shouldNotMatchWhenProfileIsInactive() {
            ProfileCondition condition = new ProfileCondition("prod");
            ConditionContext context = createContextWithProfiles("dev");
            
            assertFalse(condition.matches(context));
        }

        @Test
        @DisplayName("Should match when any profile is active (OR logic)")
        void shouldMatchWhenAnyProfileIsActive() {
            ProfileCondition condition = new ProfileCondition("dev", "test", "local");
            ConditionContext context = createContextWithProfiles("test");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when no profiles match")
        void shouldNotMatchWhenNoProfilesMatch() {
            ProfileCondition condition = new ProfileCondition("dev", "test");
            ConditionContext context = createContextWithProfiles("prod");
            
            assertFalse(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Negation Tests")
    class NegationTests {

        @Test
        @DisplayName("Should match negated profile when profile is inactive")
        void shouldMatchNegatedProfileWhenInactive() {
            ProfileCondition condition = new ProfileCondition("!prod");
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match negated profile when profile is active")
        void shouldNotMatchNegatedProfileWhenActive() {
            ProfileCondition condition = new ProfileCondition("!prod");
            ConditionContext context = createContextWithProfiles("prod");
            
            assertFalse(condition.matches(context));
        }

        @Test
        @DisplayName("Should handle negated profile with spaces")
        void shouldHandleNegatedProfileWithSpaces() {
            ProfileCondition condition = new ProfileCondition("! prod");
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should match when no profiles specified")
        void shouldMatchWhenNoProfilesSpecified() {
            ProfileCondition condition = new ProfileCondition();
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should match when null profiles array")
        void shouldMatchWhenNullProfilesArray() {
            ProfileCondition condition = new ProfileCondition((String[]) null);
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should handle empty profile string")
        void shouldHandleEmptyProfileString() {
            ProfileCondition condition = new ProfileCondition("");
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should handle null profile string in array")
        void shouldHandleNullProfileStringInArray() {
            ProfileCondition condition = new ProfileCondition("dev", null, "test");
            ConditionContext context = createContextWithProfiles("dev");
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Getter and Description Tests")
    class GetterAndDescriptionTests {

        @Test
        @DisplayName("Should return profiles array")
        void shouldReturnProfilesArray() {
            String[] profiles = {"dev", "test"};
            ProfileCondition condition = new ProfileCondition(profiles);
            
            assertArrayEquals(profiles, condition.getProfiles());
        }

        @Test
        @DisplayName("Should generate correct description")
        void shouldGenerateCorrectDescription() {
            ProfileCondition condition = new ProfileCondition("dev", "test");
            
            String description = condition.getDescription();
            
            assertTrue(description.contains("@Profile"));
            assertTrue(description.contains("dev"));
            assertTrue(description.contains("test"));
        }

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            ProfileCondition condition = new ProfileCondition("dev", "test");
            
            String str = condition.toString();
            
            assertTrue(str.contains("ProfileCondition"));
            assertTrue(str.contains("dev"));
            assertTrue(str.contains("test"));
        }
    }

    @Nested
    @DisplayName("Multiple Active Profiles")
    class MultipleActiveProfilesTests {

        @Test
        @DisplayName("Should match with multiple active profiles")
        void shouldMatchWithMultipleActiveProfiles() {
            ProfileCondition condition = new ProfileCondition("dev");
            ConditionContext context = createContextWithProfiles("dev", "local", "debug");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should match any of multiple required profiles")
        void shouldMatchAnyOfMultipleRequiredProfiles() {
            ProfileCondition condition = new ProfileCondition("staging", "prod");
            ConditionContext context = createContextWithProfiles("dev", "staging");
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Failure Reason Tests")
    class FailureReasonTests {

        @Test
        @DisplayName("Should show active vs required profiles")
        void shouldShowActiveVsRequired() {
            ProfileCondition condition = new ProfileCondition("dev", "test");
            ConditionContext context = createContextWithProfiles("prod", "secure");

            String reason = condition.getFailureReason(context);

            assertTrue(reason.contains("Profile condition not satisfied"));
            assertTrue(reason.contains("Active profiles:"));
            assertTrue(reason.contains("prod"));
            assertTrue(reason.contains("Required profiles (ANY):"));
            assertTrue(reason.contains("dev"));
            assertTrue(reason.contains("test"));
        }

        @Test
        @DisplayName("Should handle negated profiles in failure reason")
        void shouldHandleNegatedProfiles() {
            ProfileCondition condition = new ProfileCondition("!prod");
            ConditionContext context = createContextWithProfiles("prod");

            String reason = condition.getFailureReason(context);

            assertTrue(reason.contains("Active profiles:"));
            assertTrue(reason.contains("prod"));
            assertTrue(reason.contains("NOT"));
        }

        @Test
        @DisplayName("Should show no active profiles")
        void shouldShowNoActiveProfiles() {
            ProfileCondition condition = new ProfileCondition("dev");
            ConditionContext context = new ConditionContext(null, new HashSet<>());

            String reason = condition.getFailureReason(context);

            assertTrue(reason.contains("[none]"));
        }

        @Test
        @DisplayName("Should return empty when condition passes")
        void shouldReturnEmptyWhenPasses() {
            ProfileCondition condition = new ProfileCondition("dev");
            ConditionContext context = createContextWithProfiles("dev");

            String reason = condition.getFailureReason(context);

            assertEquals("", reason);
        }
    }
}
