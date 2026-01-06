package io.github.yasmramos.veld.benchmark.features.validation;

import io.github.yasmramos.veld.annotation.Singleton;

@Singleton
public class PlainValidatedBean {
    private String name;
    private String value;
    private String description;
    private String code;

    public void process(PlainValidatedBean data) {
        // Method without validation
    }
}
