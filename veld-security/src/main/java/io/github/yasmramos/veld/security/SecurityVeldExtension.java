package io.github.yasmramos.veld.security;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

/**
 * Extensión de Veld para el módulo de seguridad.
 */
public class SecurityVeldExtension implements VeldExtension {

    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            "io.github.yasmramos/veld-security",
            ExtensionPhase.VALIDATION,
            100
        );
    }

    @Override
    public void execute(VeldGraph graph, VeldProcessingContext context) {
        context.reportNote("Veld Security Extension: Validando configuraciones de seguridad...", null);
    }
}
