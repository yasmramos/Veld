package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

/**
 * Extensión de Veld para el módulo de métricas.
 */
public class MetricsVeldExtension implements VeldExtension {

    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            "io.github.yasmramos/veld-metrics",
            ExtensionPhase.ANALYSIS,
            100
        );
    }

    @Override
    public void execute(VeldGraph graph, VeldProcessingContext context) {
        context.reportNote("Veld Metrics Extension: Analizando componentes para métricas...", null);
    }
}
