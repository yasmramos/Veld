package io.github.yasmramos.veld.core.lifecycle;

import io.github.yasmramos.veld.core.bean.BeanDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class BeanInitializationOrderResolver {

    private BeanInitializationOrderResolver() {
    }

    public static List<BeanDefinition> resolve(Collection<BeanDefinition> beans) {
        if (beans == null || beans.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, BeanDefinition> beansByName = new LinkedHashMap<>();
        for (BeanDefinition bean : beans) {
            if (beansByName.put(bean.getName(), bean) != null) {
                throw new IllegalStateException("Duplicate bean name: " + bean.getName());
            }
        }

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Set<String>> dependents = new HashMap<>();
        for (String name : beansByName.keySet()) {
            inDegree.put(name, 0);
            dependents.put(name, new HashSet<>());
        }

        for (BeanDefinition bean : beansByName.values()) {
            String name = bean.getName();
            Collection<String> dependencies = bean.getDependencies();
            if (dependencies == null) {
                continue;
            }
            for (String dependency : dependencies) {
                if (dependency.equals(name)) {
                    throw new IllegalStateException("Bean '" + name + "' depends on itself");
                }
                if (!beansByName.containsKey(dependency)) {
                    throw new IllegalStateException(
                            "Bean '" + name + "' depends on unknown bean '" + dependency + "'");
                }
                if (dependents.get(dependency).add(name)) {
                    inDegree.merge(name, 1, Integer::sum);
                }
            }
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<BeanDefinition> ordered = new ArrayList<>(beansByName.size());
        while (!ready.isEmpty()) {
            String current = ready.poll();
            ordered.add(beansByName.get(current));
            List<String> next = new ArrayList<>(dependents.get(current));
            Collections.sort(next);
            for (String dependent : next) {
                int remaining = inDegree.merge(dependent, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (ordered.size() != beansByName.size()) {
            List<String> cyclic = new ArrayList<>();
            for (String name : beansByName.keySet()) {
                if (inDegree.get(name) > 0) {
                    cyclic.add(name);
                }
            }
            Collections.sort(cyclic);
            throw new IllegalStateException("Circular dependency detected among beans: " + cyclic);
        }

        return ordered;
    }
}
