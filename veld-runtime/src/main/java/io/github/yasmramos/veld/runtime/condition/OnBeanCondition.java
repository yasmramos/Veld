package io.github.yasmramos.veld.runtime.condition;

import io.github.yasmramos.veld.core.annotation.ConditionalOnBean;
import io.github.yasmramos.veld.core.annotation.ConditionalOnMissingBean;
import io.github.yasmramos.veld.runtime.container.BeanContainer;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

public class OnBeanCondition implements Condition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedElement element) {
        BeanContainer container = context.getContainer();

        ConditionalOnBean onBean = element.getAnnotation(ConditionalOnBean.class);
        if (onBean != null) {
            for (Class<?> type : onBean.value()) {
                List<String> names = container.getBeanNamesForType(type);
                if (names.isEmpty()) {
                    return ConditionOutcome.noMatch(String.format(
                        "Required bean of type '%s' not found in container", type.getName()));
                }
            }
            for (String name : onBean.name()) {
                if (!container.containsBean(name)) {
                    return ConditionOutcome.noMatch(String.format(
                        "Required bean with name '%s' not found in container", name));
                }
            }
            return ConditionOutcome.match();
        }

        ConditionalOnMissingBean onMissingBean = element.getAnnotation(ConditionalOnMissingBean.class);
        if (onMissingBean != null) {
            for (Class<?> type : onMissingBean.value()) {
                List<String> names = container.getBeanNamesForType(type);
                if (!names.isEmpty()) {
                    return ConditionOutcome.noMatch(String.format(
                        "Bean of type '%s' already registered as '%s'; condition requires it to be absent",
                        type.getName(), String.join(", ", names)));
                }
            }
            for (String name : onMissingBean.name()) {
                if (container.containsBean(name)) {
                    return ConditionOutcome.noMatch(String.format(
                        "Bean with name '%s' already registered; condition requires it to be absent", name));
                }
            }
            return ConditionOutcome.match();
        }

        return ConditionOutcome.match();
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedElement element) {
        return getMatchOutcome(context, element).isMatch();
    }
}
