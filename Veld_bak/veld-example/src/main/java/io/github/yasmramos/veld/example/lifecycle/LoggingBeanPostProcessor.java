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
package io.github.yasmramos.veld.example.lifecycle;

import io.github.yasmramos.veld.runtime.lifecycle.BeanPostProcessor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Example BeanPostProcessor that logs bean initialization.
 */
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    
    private final AtomicInteger beforeCount = new AtomicInteger(0);
    private final AtomicInteger afterCount = new AtomicInteger(0);
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        int count = beforeCount.incrementAndGet();
        System.out.println("  [BeanPostProcessor] Before init #" + count + ": " + beanName + 
                " (" + bean.getClass().getSimpleName() + ")");
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        int count = afterCount.incrementAndGet();
        System.out.println("  [BeanPostProcessor] After init #" + count + ": " + beanName + 
                " (" + bean.getClass().getSimpleName() + ")");
        return bean;
    }
    
    @Override
    public int getOrder() {
        return 0; // Default order
    }
    
    public int getBeforeCount() {
        return beforeCount.get();
    }
    
    public int getAfterCount() {
        return afterCount.get();
    }
}
