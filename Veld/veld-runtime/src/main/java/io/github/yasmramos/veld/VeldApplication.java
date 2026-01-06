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
package io.github.yasmramos.veld;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.runtime.event.Event;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.event.ObjectLessEventBus;

/**
 * Main application class for Veld Framework.
 *
 * <p>This class serves as an entry point for GraalVM native image compilation.
 * When compiled with GraalVM, the framework provides near-instant startup
 * and minimal memory footprint.</p>
 *
 * <p><b>GraalVM Native Image Benefits:</b></p>
 * <ul>
 *   <li>Near-instant startup time</li>
 *   <li>Reduced memory footprint</li>
 *   <li>Instant peak performance</li>
 *   <li>Smaller executable size</li>
 * </ul>
 *
 * <p><b>Example Native Build:</b></p>
 * <pre>{@code
 * # Install GraalVM
 * gu install native-image
 *
 * # Build native executable
 * native-image -jar veld-runtime-1.0.3.jar veld-runtime
 *
 * # Run the native executable
 * ./veld-runtime
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.3
 */
@Component
public class VeldApplication {

    private final EventBus eventBus;

    /**
     * Creates a new VeldApplication instance.
     *
     * @param eventBus the event bus for event handling
     */
    public VeldApplication(EventBus eventBus) {
        this.eventBus = eventBus;
        System.out.println("[Veld] Application initialized");
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Veld Framework - Native Image Ready");
        System.out.println("========================================");
        System.out.println();

        // Verify GraalVM
        String vendor = System.getProperty("java.vendor");
        String version = System.getProperty("java.version");
        System.out.println("[Veld] Java Vendor: " + vendor);
        System.out.println("[Veld] Java Version: " + version);
        System.out.println();

        // Check if running as native image
        boolean isNativeImage = Boolean.getBoolean("org.graalvm.nativeimage.imagecode");
        if (isNativeImage) {
            System.out.println("[Veld] Running as GraalVM Native Image!");
        } else {
            System.out.println("[Veld] Running on standard JVM");
            System.out.println("[Veld] To build native image, use:");
            System.out.println("  gu install native-image");
            System.out.println("  native-image -jar veld-runtime-1.0.3.jar");
        }
        System.out.println();

        System.out.println("[Veld] Framework initialized successfully!");
        System.out.println("[Veld] Ready for dependency injection and event handling.");
        System.out.println();
    }

    /**
     * Example event handler for demonstration.
     *
     * @param event the event received
     */
    @Subscribe
    public void onApplicationEvent(Event event) {
        System.out.println("[Veld] Received event: " + event.getEventType());
    }
}
