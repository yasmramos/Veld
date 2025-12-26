package io.github.yasmramos.veld.weaver;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Maven plugin that weaves bytecode to add synthetic setter methods for private field injection.
 * 
 * <p>This plugin should be executed after compilation (process-classes phase) to transform
 * compiled classes and add {@code __di_set_*} methods for fields annotated with @Inject.
 * 
 * <p>Usage in pom.xml:
 * <pre>{@code
 * <build>
 *     <plugins>
 *         <plugin>
 *             <groupId>com.veld</groupId>
 *             <artifactId>veld-weaver</artifactId>
 *             <version>${veld.version}</version>
 *             <executions>
 *                 <execution>
 *                     <goals>
 *                         <goal>weave</goal>
 *                     </goals>
 *                 </execution>
 *             </executions>
 *         </plugin>
 *     </plugins>
 * </build>
 * }</pre>
 * 
 * <p>The plugin will:
 * <ol>
 *   <li>Scan all .class files in target/classes</li>
 *   <li>Find fields with @Inject or @Value annotations</li>
 *   <li>Generate synthetic setter methods for private fields</li>
 *   <li>Rewrite the .class files with the new methods</li>
 * </ol>
 */
@Mojo(name = "weave", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class WeaverMojo extends AbstractMojo {
    
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    
    /**
     * The directory containing compiled classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;
    
    /**
     * Whether to skip weaving.
     */
    @Parameter(property = "veld.weaver.skip", defaultValue = "false")
    private boolean skip;
    
    /**
     * Whether to show verbose output.
     */
    @Parameter(property = "veld.weaver.verbose", defaultValue = "false")
    private boolean verbose;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Veld weaving is skipped");
            return;
        }
        
        if (!classesDirectory.exists()) {
            getLog().info("Classes directory does not exist, skipping weaving: " + classesDirectory);
            return;
        }
        
        getLog().info("Veld Weaver: Processing classes in " + classesDirectory);
        
        try {
            FieldInjectorWeaver weaver = new FieldInjectorWeaver();
            Path classesPath = classesDirectory.toPath();
            
            List<FieldInjectorWeaver.WeavingResult> results = weaver.weaveDirectory(classesPath);
            
            int modifiedCount = 0;
            int errorCount = 0;
            
            for (FieldInjectorWeaver.WeavingResult result : results) {
                if (result.hasError()) {
                    getLog().error("Failed to weave " + result.className() + ": " + result.errorMessage());
                    errorCount++;
                } else if (result.modified()) {
                    modifiedCount++;
                    if (verbose) {
                        getLog().info("  Woven: " + result.className().replace('/', '.'));
                        for (String setter : result.addedSetters()) {
                            getLog().info("    + " + setter + "()");
                        }
                    }
                }
            }
            
            if (modifiedCount > 0) {
                getLog().info("Veld Weaver: " + modifiedCount + " class(es) enhanced with injection setters");
            } else {
                getLog().info("Veld Weaver: No classes required weaving");
            }
            
            if (errorCount > 0) {
                throw new MojoFailureException("Weaving failed for " + errorCount + " class(es)");
            }
            
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to weave classes", e);
        }
    }
    
    // Setters for testing
    
    void setClassesDirectory(File classesDirectory) {
        this.classesDirectory = classesDirectory;
    }
    
    void setSkip(boolean skip) {
        this.skip = skip;
    }
    
    void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
