package io.github.yasmramos.veld.aop;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;

/**
 * Implementación concreta de la extensión AOP para Veld.
 */
public class AopVeldExtension extends AopExtension {

    @Override
    public ExtensionDescriptor getDescriptor() {
        return new ExtensionDescriptor(
            "io.github.yasmramos/veld-aop",
            ExtensionPhase.GENERATION,
            10
        );
    }
}
