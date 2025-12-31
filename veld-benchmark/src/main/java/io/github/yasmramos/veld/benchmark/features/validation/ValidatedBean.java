package io.github.yasmramos.veld.benchmark.features.validation;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Valid;
import io.github.yasmramos.veld.annotation.NotNull;
import io.github.yasmramos.veld.annotation.NotEmpty;
import io.github.yasmramos.veld.annotation.Size;
import io.github.yasmramos.veld.annotation.Pattern;

@Singleton
public class ValidatedBean {
    @NotNull
    private String name;

    @NotEmpty
    private String value;

    @Size(min = 1, max = 100)
    private String description;

    @Pattern(regexp = "^[a-zA-Z]+$")
    private String code;

    public ValidatedBean() {
        // Public no-arg constructor required by Veld
    }

    public void processWithValidation(@Valid ValidatedBean data) {
        // Method with validation parameter
    }

    public void processWithoutValidation(ValidatedBean data) {
        // Method without validation
    }
}
