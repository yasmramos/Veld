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
package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConditionInfo} and its nested condition types.
 */
@DisplayName("ConditionInfo Tests")
class ConditionInfoTest {

    private ConditionInfo conditionInfo;

    @BeforeEach
    void setUp() {
        conditionInfo = new ConditionInfo();
    }

    @Nested
    @DisplayName("Empty Condition Tests")
    class EmptyConditionTests {

        @Test
        @DisplayName("should have no conditions initially")
        void shouldHaveNoConditionsInitially() {
            assertFalse(conditionInfo.hasConditions());
        }

        @Test
        @DisplayName("should return empty property conditions list")
        void shouldReturnEmptyPropertyConditionsList() {
            assertTrue(conditionInfo.getPropertyConditions().isEmpty());
        }

        @Test
        @DisplayName("should return empty class conditions list")
        void shouldReturnEmptyClassConditionsList() {
            assertTrue(conditionInfo.getClassConditions().isEmpty());
        }

        @Test
        @DisplayName("should return empty missing bean conditions list")
        void shouldReturnEmptyMissingBeanConditionsList() {
            assertTrue(conditionInfo.getMissingBeanConditions().isEmpty());
        }

        @Test
        @DisplayName("should return empty profile conditions list")
        void shouldReturnEmptyProfileConditionsList() {
            assertTrue(conditionInfo.getProfileConditions().isEmpty());
        }
    }

    @Nested
    @DisplayName("Property Condition Tests")
    class PropertyConditionTests {

        @Test
        @DisplayName("should add property condition")
        void shouldAddPropertyCondition() {
            conditionInfo.addPropertyCondition("app.feature.enabled", "true", false);
            
            assertTrue(conditionInfo.hasConditions());
            assertEquals(1, conditionInfo.getPropertyConditions().size());
        }

        @Test
        @DisplayName("should store property condition values correctly")
        void shouldStorePropertyConditionValuesCorrectly() {
            conditionInfo.addPropertyCondition("my.property", "expectedValue", true);
            
            ConditionInfo.PropertyConditionInfo info = conditionInfo.getPropertyConditions().get(0);
            assertEquals("my.property", info.name());
            assertEquals("expectedValue", info.havingValue());
            assertTrue(info.matchIfMissing());
        }

        @Test
        @DisplayName("should add multiple property conditions")
        void shouldAddMultiplePropertyConditions() {
            conditionInfo.addPropertyCondition("prop1", "val1", false);
            conditionInfo.addPropertyCondition("prop2", "val2", true);
            conditionInfo.addPropertyCondition("prop3", "val3", false);
            
            assertEquals(3, conditionInfo.getPropertyConditions().size());
        }

        @Test
        @DisplayName("should handle empty having value")
        void shouldHandleEmptyHavingValue() {
            conditionInfo.addPropertyCondition("feature.flag", "", false);
            
            ConditionInfo.PropertyConditionInfo info = conditionInfo.getPropertyConditions().get(0);
            assertEquals("", info.havingValue());
        }
    }

    @Nested
    @DisplayName("Class Condition Tests")
    class ClassConditionTests {

        @Test
        @DisplayName("should add class condition")
        void shouldAddClassCondition() {
            List<String> classes = Arrays.asList("com.example.OptionalDependency");
            conditionInfo.addClassCondition(classes);
            
            assertTrue(conditionInfo.hasConditions());
            assertEquals(1, conditionInfo.getClassConditions().size());
        }

        @Test
        @DisplayName("should store class names correctly")
        void shouldStoreClassNamesCorrectly() {
            List<String> classes = Arrays.asList(
                "com.example.ClassA",
                "com.example.ClassB",
                "com.example.ClassC"
            );
            conditionInfo.addClassCondition(classes);
            
            ConditionInfo.ClassConditionInfo info = conditionInfo.getClassConditions().get(0);
            assertEquals(3, info.classNames().size());
            assertTrue(info.classNames().contains("com.example.ClassA"));
            assertTrue(info.classNames().contains("com.example.ClassB"));
            assertTrue(info.classNames().contains("com.example.ClassC"));
        }

        @Test
        @DisplayName("should add multiple class conditions")
        void shouldAddMultipleClassConditions() {
            conditionInfo.addClassCondition(Arrays.asList("ClassA"));
            conditionInfo.addClassCondition(Arrays.asList("ClassB", "ClassC"));
            
            assertEquals(2, conditionInfo.getClassConditions().size());
        }
    }

    @Nested
    @DisplayName("Missing Bean Condition Tests")
    class MissingBeanConditionTests {

        @Test
        @DisplayName("should add missing bean type condition")
        void shouldAddMissingBeanTypeCondition() {
            List<String> types = Arrays.asList("com.example.MyService");
            conditionInfo.addMissingBeanTypeCondition(types);
            
            assertTrue(conditionInfo.hasConditions());
            assertEquals(1, conditionInfo.getMissingBeanConditions().size());
        }

        @Test
        @DisplayName("should store bean types correctly")
        void shouldStoreBeanTypesCorrectly() {
            List<String> types = Arrays.asList(
                "com.example.ServiceA",
                "com.example.ServiceB"
            );
            conditionInfo.addMissingBeanTypeCondition(types);
            
            ConditionInfo.MissingBeanConditionInfo info = conditionInfo.getMissingBeanConditions().get(0);
            assertEquals(2, info.beanTypes().size());
            assertTrue(info.beanNames().isEmpty());
        }

        @Test
        @DisplayName("should add missing bean name condition")
        void shouldAddMissingBeanNameCondition() {
            List<String> names = Arrays.asList("myBean", "anotherBean");
            conditionInfo.addMissingBeanNameCondition(names);
            
            assertTrue(conditionInfo.hasConditions());
            ConditionInfo.MissingBeanConditionInfo info = conditionInfo.getMissingBeanConditions().get(0);
            assertEquals(2, info.beanNames().size());
            assertTrue(info.beanTypes().isEmpty());
        }

        @Test
        @DisplayName("should add multiple missing bean conditions")
        void shouldAddMultipleMissingBeanConditions() {
            conditionInfo.addMissingBeanTypeCondition(Arrays.asList("TypeA"));
            conditionInfo.addMissingBeanNameCondition(Arrays.asList("beanB"));
            
            assertEquals(2, conditionInfo.getMissingBeanConditions().size());
        }
    }

    @Nested
    @DisplayName("Profile Condition Tests")
    class ProfileConditionTests {

        @Test
        @DisplayName("should add profile condition")
        void shouldAddProfileCondition() {
            List<String> profiles = Arrays.asList("dev", "test");
            conditionInfo.addProfileCondition(profiles);
            
            assertTrue(conditionInfo.hasConditions());
            assertEquals(1, conditionInfo.getProfileConditions().size());
        }

        @Test
        @DisplayName("should store profiles correctly")
        void shouldStoreProfilesCorrectly() {
            List<String> profiles = Arrays.asList("production", "cloud");
            conditionInfo.addProfileCondition(profiles);
            
            ConditionInfo.ProfileConditionInfo info = conditionInfo.getProfileConditions().get(0);
            assertEquals(2, info.profiles().size());
            assertTrue(info.profiles().contains("production"));
            assertTrue(info.profiles().contains("cloud"));
        }

        @Test
        @DisplayName("should add multiple profile conditions")
        void shouldAddMultipleProfileConditions() {
            conditionInfo.addProfileCondition(Arrays.asList("dev"));
            conditionInfo.addProfileCondition(Arrays.asList("staging"));
            conditionInfo.addProfileCondition(Arrays.asList("prod"));
            
            assertEquals(3, conditionInfo.getProfileConditions().size());
        }
    }

    @Nested
    @DisplayName("Combined Condition Tests")
    class CombinedConditionTests {

        @Test
        @DisplayName("should handle multiple condition types")
        void shouldHandleMultipleConditionTypes() {
            conditionInfo.addPropertyCondition("feature.enabled", "true", false);
            conditionInfo.addClassCondition(Arrays.asList("com.optional.Library"));
            conditionInfo.addMissingBeanTypeCondition(Arrays.asList("com.example.Backup"));
            conditionInfo.addProfileCondition(Arrays.asList("production"));
            
            assertTrue(conditionInfo.hasConditions());
            assertEquals(1, conditionInfo.getPropertyConditions().size());
            assertEquals(1, conditionInfo.getClassConditions().size());
            assertEquals(1, conditionInfo.getMissingBeanConditions().size());
            assertEquals(1, conditionInfo.getProfileConditions().size());
        }

        @Test
        @DisplayName("hasConditions should return true for any single condition type")
        void hasConditionsShouldReturnTrueForAnySingleConditionType() {
            ConditionInfo withProperty = new ConditionInfo();
            withProperty.addPropertyCondition("prop", "val", false);
            assertTrue(withProperty.hasConditions());
            
            ConditionInfo withClass = new ConditionInfo();
            withClass.addClassCondition(Arrays.asList("Class"));
            assertTrue(withClass.hasConditions());
            
            ConditionInfo withMissingBean = new ConditionInfo();
            withMissingBean.addMissingBeanTypeCondition(Arrays.asList("Type"));
            assertTrue(withMissingBean.hasConditions());
            
            ConditionInfo withProfile = new ConditionInfo();
            withProfile.addProfileCondition(Arrays.asList("profile"));
            assertTrue(withProfile.hasConditions());
        }
    }
}
