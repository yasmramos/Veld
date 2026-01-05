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
package io.github.yasmramos.veld.aop.pointcut;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A composite pointcut supporting logical operators (AND, OR, NOT).
 *
 * <p>Allows combining multiple pointcut expressions with boolean logic.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Match service methods OR dao methods
 * CompositePointcut pointcut = CompositePointcut.parse(
 *     "execution(* com.example.service.*.*(..)) || execution(* com.example.dao.*.*(..))");
 *
 * // Match service methods that are NOT getters
 * CompositePointcut pointcut = CompositePointcut.parse(
 *     "execution(* com.example.service.*.*(..)) &amp;&amp; !execution(* *.get*(..))");
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class CompositePointcut {

    private final String expression;
    private final Node root;

    /**
     * Node types for the expression tree.
     */
    private enum NodeType {
        AND, OR, NOT, LEAF
    }

    /**
     * Expression tree node.
     */
    private static class Node {
        NodeType type;
        PointcutExpression pointcut;
        Node left;
        Node right;

        Node(PointcutExpression pointcut) {
            this.type = NodeType.LEAF;
            this.pointcut = pointcut;
        }

        Node(NodeType type, Node left, Node right) {
            this.type = type;
            this.left = left;
            this.right = right;
        }

        Node(NodeType type, Node operand) {
            this.type = type;
            this.left = operand;
        }

        boolean matches(Method method) {
            switch (type) {
                case LEAF:
                    return pointcut.matches(method);
                case AND:
                    return left.matches(method) && right.matches(method);
                case OR:
                    return left.matches(method) || right.matches(method);
                case NOT:
                    return !left.matches(method);
                default:
                    return false;
            }
        }

        boolean couldMatch(Class<?> clazz) {
            switch (type) {
                case LEAF:
                    return pointcut.couldMatch(clazz);
                case AND:
                    return left.couldMatch(clazz) && right.couldMatch(clazz);
                case OR:
                    return left.couldMatch(clazz) || right.couldMatch(clazz);
                case NOT:
                    return true; // Conservative: always check
                default:
                    return true;
            }
        }
    }

    /**
     * Parses a composite pointcut expression.
     *
     * @param expression the expression (may include &&, ||, !)
     * @return the parsed composite pointcut
     */
    public static CompositePointcut parse(String expression) {
        return new CompositePointcut(expression);
    }

    private CompositePointcut(String expression) {
        this.expression = expression.trim();
        this.root = parseExpression(this.expression);
    }

    private Node parseExpression(String expr) {
        expr = expr.trim();
        
        // Handle OR (lowest precedence)
        int orIndex = findOperator(expr, "||");
        if (orIndex != -1) {
            String left = expr.substring(0, orIndex).trim();
            String right = expr.substring(orIndex + 2).trim();
            return new Node(NodeType.OR, parseExpression(left), parseExpression(right));
        }

        // Handle AND
        int andIndex = findOperator(expr, "&&");
        if (andIndex != -1) {
            String left = expr.substring(0, andIndex).trim();
            String right = expr.substring(andIndex + 2).trim();
            return new Node(NodeType.AND, parseExpression(left), parseExpression(right));
        }

        // Handle NOT
        if (expr.startsWith("!")) {
            return new Node(NodeType.NOT, parseExpression(expr.substring(1).trim()));
        }

        // Handle parentheses
        if (expr.startsWith("(") && findMatchingParen(expr, 0) == expr.length() - 1) {
            return parseExpression(expr.substring(1, expr.length() - 1));
        }

        // Check if it's a reference to another pointcut (method name)
        if (expr.matches("[a-zA-Z_][a-zA-Z0-9_]*\\(\\)")) {
            // This is a pointcut reference, for now treat as a simple pattern
            // In a full implementation, this would look up the pointcut definition
            return new Node(PointcutExpression.parse("execution(* *." + 
                expr.substring(0, expr.length() - 2) + "(..))"));
        }

        // Leaf node - parse as simple pointcut
        return new Node(PointcutExpression.parse(expr));
    }

    /**
     * Finds an operator at the top level (not inside parentheses).
     */
    private int findOperator(String expr, String operator) {
        int depth = 0;
        for (int i = 0; i < expr.length() - operator.length() + 1; i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && expr.substring(i).startsWith(operator)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the matching closing parenthesis.
     */
    private int findMatchingParen(String expr, int start) {
        int depth = 0;
        for (int i = start; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /**
     * Tests if this pointcut matches a method.
     *
     * @param method the method to test
     * @return true if the pointcut matches
     */
    public boolean matches(Method method) {
        return root.matches(method);
    }

    /**
     * Tests if this pointcut could match any method in a class.
     *
     * @param clazz the class to test
     * @return true if any method could match
     */
    public boolean couldMatch(Class<?> clazz) {
        return root.couldMatch(clazz);
    }

    /**
     * Returns the original expression.
     *
     * @return the expression string
     */
    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "CompositePointcut{" + expression + "}";
    }
}
