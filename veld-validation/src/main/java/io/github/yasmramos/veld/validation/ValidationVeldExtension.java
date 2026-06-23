package io.github.yasmramos.veld.validation;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

/**
 * Extensión de Veld para el módulo de validación.
 */
public class ValidationVeldExtension implements VeldExtension {

    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            "io.github.yasmramos/veld-validation",
            ExtensionPhase.VALIDATION,
            200
        );
    }

    @Override
    public void execute(VeldGraph graph, VeldProcessingContext context) {
        context.reportNote("Veld Validation Extension: Ejecutando validaciones de componentes...", null);
    }
}
