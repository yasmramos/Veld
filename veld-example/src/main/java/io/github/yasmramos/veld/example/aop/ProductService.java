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
package io.github.yasmramos.veld.example.aop;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.aop.interceptor.Logged;
import io.github.yasmramos.veld.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Example service demonstrating AOP with product operations.
 *
 * <p>Methods use @Logged, @Transactional interceptor bindings
 * and are also intercepted by LoggingAspect.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Component
public class ProductService {

    private final Map<String, Product> products = new HashMap<>();

    /**
     * Simple product record.
     */
    public static class Product {
        public final String id;
        public final String name;
        public final double price;

        public Product(String id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return String.format("Product{id='%s', name='%s', price=%.2f}", id, name, price);
        }
    }

    /**
     * Creates a new product.
     */
    @Transactional
    @Logged
    public Product createProduct(String id, String name, double price) {
        if (products.containsKey(id)) {
            throw new IllegalArgumentException("Product already exists: " + id);
        }
        Product product = new Product(id, name, price);
        products.put(id, product);
        return product;
    }

    /**
     * Finds a product by ID.
     */
    @Logged(logArgs = true, logResult = true)
    public Product findProduct(String id) {
        Product product = products.get(id);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        return product;
    }

    /**
     * Updates product price.
     */
    @Transactional
    public Product updatePrice(String id, double newPrice) {
        Product existing = products.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        Product updated = new Product(id, existing.name, newPrice);
        products.put(id, updated);
        return updated;
    }

    /**
     * Deletes a product.
     */
    @Transactional
    @Logged
    public boolean deleteProduct(String id) {
        return products.remove(id) != null;
    }

    /**
     * Gets the count of products.
     */
    public int getProductCount() {
        return products.size();
    }
}
