package io.github.yasmramos.veld.core.lifecycle;

import io.github.yasmramos.veld.core.bean.BeanDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BeanInitializationOrderResolverTest {

    private final BeanInitializationOrderResolver resolver = new BeanInitializationOrderResolver();

    @Test
    void resolvesLinearChainCDependsOnBDependsOnA() {
        BeanDefinition a = mockBean("a", Collections.emptySet(), Collections.emptySet(), 0, false);
        BeanDefinition b = mockBean("b", set("a"), Collections.emptySet(), 0, false);
        BeanDefinition c = mockBean("c", set("b"), Collections.emptySet(), 0, false);

        List<BeanDefinition> result = resolver.resolve(Arrays.asList(c, b, a));

        assertEquals(Arrays.asList("a", "b", "c"), names(result));
    }

    @Test
    void resolvesMultiRootGraph() {
        BeanDefinition a = mockBean("a", Collections.emptySet(), Collections.emptySet(), 0, false);
        BeanDefinition b = mockBean("b", Collections.emptySet(), Collections.emptySet(), 0, false);
        BeanDefinition c = mockBean("c", set("a"), Collections.emptySet(), 0, false);
        BeanDefinition d = mockBean("d", set("b"), Collections.emptySet(), 0, false);
        BeanDefinition e = mockBean("e", set("c", "d"), Collections.emptySet(), 0, false);

        List<BeanDefinition> result = resolver.resolve(Arrays.asList(e, d, c, b, a));
        List<String> ordered = names(result);

        assertTrue(ordered.indexOf("a") < ordered.indexOf("c"));
        assertTrue(ordered.indexOf("b") < ordered.indexOf("d"));
        assertTrue(ordered.indexOf("c") < ordered.indexOf("e"));
        assertTrue(ordered.indexOf("d") < ordered.indexOf("e"));
    }

    @Test
    void breaksTiesUsingOrderAnnotation() {
        BeanDefinition a = mockBean("a", Collections.emptySet(), Collections.emptySet(), 10, false);
        BeanDefinition b = mockBean("b", Collections.emptySet(), Collections.emptySet(), 1, false);
        BeanDefinition c = mockBean("c", Collections.emptySet(), Collections.emptySet(), 5, false);

        List<BeanDefinition> result = resolver.resolve(Arrays.asList(a, b, c));

        assertEquals(Arrays.asList("b", "c", "a"), names(result));
    }

    @Test
    void honoursDependsOnOnlyEdges() {
        BeanDefinition a = mockBean("a", Collections.emptySet(), Collections.emptySet(), 0, false);
        BeanDefinition b = mockBean("b", Collections.emptySet(), set("a"), 0, false);

        List<BeanDefinition> result = resolver.resolve(Arrays.asList(b, a));

        assertEquals(Arrays.asList("a", "b"), names(result));
    }

    @Test
    void detectsCycleAndReportsParticipatingBeans() {
        BeanDefinition a = mockBean("a", set("b"), Collections.emptySet(), 0, false);
        BeanDefinition b = mockBean("b", set("c"), Collections.emptySet(), 0, false);
        BeanDefinition c = mockBean("c", set("a"), Collections.emptySet(), 0, false);

        CircularDependencyException ex = assertThrows(
                CircularDependencyException.class,
                () -> resolver.resolve(Arrays.asList(a, b, c))
        );

        String message = ex.getMessage();
        assertTrue(message.contains("a"), () -> "expected message to reference 'a': " + message);
        assertTrue(message.contains("b"), () -> "expected message to reference 'b': " + message);
        assertTrue(message.contains("c"), () -> "expected message to reference 'c': " + message);
    }

    @Test
    void lazyBeanBreaksCycle() {
        BeanDefinition a = mockBean("a", set("b"), Collections.emptySet(), 0, false);
        BeanDefinition b = mockBean("b", set("a"), Collections.emptySet(), 0, true);

        List<BeanDefinition> result = resolver.resolve(Arrays.asList(a, b));
        List<String> ordered = names(result);

        assertEquals(2, ordered.size());
        assertTrue(ordered.contains("a"));
        assertTrue(ordered.contains("b"));
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static List<String> names(List<BeanDefinition> beans) {
        return beans.stream().map(BeanDefinition::getName).collect(Collectors.toList());
    }

    private static BeanDefinition mockBean(String name, Set<String> dependencies, Set<String> dependsOn, int order, boolean lazy) {
        BeanDefinition bd = mock(BeanDefinition.class);
        lenient().when(bd.getName()).thenReturn(name);
        lenient().when(bd.getDependencies()).thenReturn(dependencies);
        lenient().when(bd.getDependsOn()).thenReturn(dependsOn);
        lenient().when(bd.getOrder()).thenReturn(order);
        lenient().when(bd.isLazy()).thenReturn(lazy);
        return bd;
    }
}
