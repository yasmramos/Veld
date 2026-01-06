package io.github.yasmramos.veld.runtime.aop;

import java.util.function.Supplier;

/**
 * Represents a method invocation that can be intercepted.
 */
public record MethodInvocation(
    String methodName,
    Class<?> targetClass,
    Supplier<Object> target
) {
    /** Execute the original method */
    public Object proceed() {
        return target.get();
    }
}
