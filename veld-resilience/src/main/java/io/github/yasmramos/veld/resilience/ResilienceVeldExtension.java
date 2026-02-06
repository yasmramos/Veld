package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

/**
 * Extensión de Veld para el módulo de resiliencia.
 */
public class ResilienceVeldExtension implements VeldExtension {

    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            "io.github.yasmramos/veld-resilience",
            ExtensionPhase.ANALYSIS,
            300
        );
    }

    @Override
    public void execute(VeldGraph graph, VeldProcessingContext context) {
        context.reportNote("Veld Resilience Extension: Analizando patrones de resiliencia...", null);
    }
}
