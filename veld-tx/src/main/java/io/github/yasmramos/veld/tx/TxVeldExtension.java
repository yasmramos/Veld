package io.github.yasmramos.veld.tx;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

/**
 * Extensión de Veld para el módulo de transacciones.
 */
public class TxVeldExtension implements VeldExtension {

    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            "io.github.yasmramos/veld-tx",
            ExtensionPhase.ANALYSIS,
            400
        );
    }

    @Override
    public void execute(VeldGraph graph, VeldProcessingContext context) {
        context.reportNote("Veld Transaction Extension: Analizando gestión transaccional...", null);
    }
}
