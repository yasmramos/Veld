package io.github.yasmramos.veld.maven;

import io.github.yasmramos.veld.weaver.FieldInjectorWeaver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.SelectorUtils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified Veld Maven plugin that handles compilation with annotation processing
 * and bytecode weaving in a single goal.
 */
@Mojo(
    name = "compile",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
public class VeldCompileMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true)
    private List<String> compileSourceRoots;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true)
    private List<String> classpathElements;

    @Parameter(property = "maven.compiler.source", defaultValue = "11")
    private String source;

    @Parameter(property = "maven.compiler.target", defaultValue = "11")
    private String target;

    @Parameter(property = "veld.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "veld.verbose", defaultValue = "false")
    private boolean verbose;

    @Parameter
    private List<String> compilerArgs;

    /* List of exclusion patterns to skip during Veld processing. */
    @Parameter
    private List<String> excludes;

    // Setter for testing
    void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Veld compilation is skipped");
            return;
        }

        getLog().info("Veld Maven Plugin: Compiling and weaving...");

        try {
            compile();
            weave();
            getLog().info("Veld Maven Plugin: Build complete");
        } catch (Exception e) {
            throw new MojoExecutionException("Veld compilation failed", e);
        }
    }

    private void compile() throws MojoExecutionException {
        getLog().info("  Phase 1: Compiling with Veld annotation processor...");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new MojoExecutionException(
                "No Java compiler available. Ensure you're running with a JDK, not a JRE.");
        }

        // Collect source files
        List<File> sourceFiles = new ArrayList<>();
        boolean hasModuleInfo = false;
        
        for (String sourceRoot : compileSourceRoots) {
            File root = new File(sourceRoot);
            if (root.exists()) {
                collectJavaFiles(root, sourceFiles);
                if (new File(root, "module-info.java").exists()) {
                    hasModuleInfo = true;
                }
            }
        }

        if (sourceFiles.isEmpty()) {
            getLog().info("  No source files to compile");
            return;
        }

        getLog().info("  Found " + sourceFiles.size() + " source file(s)");

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDirectory));

            List<File> classpathFiles = classpathElements.stream()
                .map(File::new)
                .filter(File::exists)
                .collect(Collectors.toList());

            // For modular projects, set both module-path and class-path
            if (hasModuleInfo) {
                fileManager.setLocation(StandardLocation.MODULE_PATH, classpathFiles);
            }
            fileManager.setLocation(StandardLocation.CLASS_PATH, classpathFiles);

            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            List<String> options = new ArrayList<>();
            options.add("-source");
            options.add(source);
            options.add("-target");
            options.add(target);
            options.add("-proc:full");
            
            // Add module path via command line option for modular projects
            if (hasModuleInfo && !classpathFiles.isEmpty()) {
                options.add("--module-path");
                options.add(classpathFiles.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator)));
            }
            
            if (compilerArgs != null) {
                options.addAll(compilerArgs);
            }

            if (verbose) {
                getLog().info("  Compiler options: " + options);
            }

            StringWriter output = new StringWriter();
            JavaCompiler.CompilationTask task = compiler.getTask(
                output, fileManager, diagnostics, options, null, compilationUnits);

            boolean success = task.call();

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                String message = formatDiagnostic(diagnostic);
                switch (diagnostic.getKind()) {
                    case ERROR:
                        getLog().error(message);
                        break;
                    case WARNING:
                    case MANDATORY_WARNING:
                        getLog().warn(message);
                        break;
                    default:
                        if (verbose) {
                            getLog().info(message);
                        }
                }
            }

            if (!success) {
                throw new MojoExecutionException("Compilation failed. See errors above.");
            }

            getLog().info("  Compilation successful");

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to setup compilation", e);
        } finally {
            try {
                fileManager.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void weave() throws MojoExecutionException {
        getLog().info("  Phase 2: Weaving bytecode...");

        if (!outputDirectory.exists()) {
            getLog().info("  No classes to weave");
            return;
        }

        try {
            FieldInjectorWeaver weaver = new FieldInjectorWeaver();
            List<FieldInjectorWeaver.WeavingResult> results = weaver.weaveDirectory(outputDirectory.toPath());

            int modifiedCount = 0;
            int errorCount = 0;
            int excludedCount = 0;

            for (FieldInjectorWeaver.WeavingResult result : results) {
                // Check if this class should be excluded from weaving
                String relativeClassPath = result.getClassName() + ".class";
                if (!shouldProcessClassFile(relativeClassPath)) {
                    excludedCount++;
                    if (verbose) {
                        getLog().info("  Excluded from weaving: " + result.getClassName().replace('/', '.'));
                    }
                    continue;
                }

                if (result.hasError()) {
                    getLog().error("  Failed to weave " + result.getClassName() + ": " + result.getErrorMessage());
                    errorCount++;
                } else if (result.wasModified()) {
                    modifiedCount++;
                    if (verbose) {
                        getLog().info("    Woven: " + result.getClassName().replace('/', '.'));
                        for (String setter : result.getAddedSetters()) {
                            getLog().info("      + " + setter + "()");
                        }
                    }
                }
            }

            if (modifiedCount > 0) {
                getLog().info("  " + modifiedCount + " class(es) enhanced");
            }
            
            if (excludedCount > 0) {
                getLog().info("  " + excludedCount + " class(es) excluded from weaving");
            }

            if (errorCount > 0) {
                throw new MojoExecutionException("Weaving failed for " + errorCount + " class(es)");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to weave classes", e);
        }
    }

    private void collectJavaFiles(File directory, List<File> result) {
        collectJavaFiles(directory, result, null);
    }

    private void collectJavaFiles(File directory, List<File> result, File sourceRoot) {
        File[] files = directory.listFiles();
        if (files == null) return;

        // Establish source root for first call
        if (sourceRoot == null) {
            sourceRoot = directory;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(file, result, sourceRoot);
            } else if (file.getName().endsWith(".java")) {
                if (shouldProcessFile(file, sourceRoot, ".java")) {
                    result.add(file);
                } else if (verbose) {
                    getLog().info("  Excluded from compilation: " + getRelativePath(file, sourceRoot));
                }
            }
        }
    }

    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        StringBuilder sb = new StringBuilder();
        
        if (diagnostic.getSource() != null) {
            String sourceName = diagnostic.getSource().getName();
            int lastSlash = Math.max(sourceName.lastIndexOf('/'), sourceName.lastIndexOf('\\'));
            if (lastSlash >= 0) {
                sourceName = sourceName.substring(lastSlash + 1);
            }
            sb.append(sourceName);
            
            if (diagnostic.getLineNumber() > 0) {
                sb.append(":").append(diagnostic.getLineNumber());
            }
            sb.append(": ");
        }
        
        sb.append(diagnostic.getMessage(null));
        return sb.toString();
    }

    /**
     * Checks if a source file should be processed based on the exclude patterns.
     *
     * @param file The source file to check
     * @param sourceRoot The source root directory
     * @param extension The file extension to use for pattern matching
     * @return true if the file should be processed, false if excluded
     */
    boolean shouldProcessFile(File file, File sourceRoot, String extension) {
        if (excludes == null || excludes.isEmpty()) {
            return true;
        }

        String relativePath = getRelativePath(file, sourceRoot);
        String patternPath = relativePath.replace(extension, ".class");

        return !isExcluded(patternPath);
    }

    /**
     * Checks if a class file should be processed during weaving based on exclude patterns.
     *
     * @param relativeClassPath The relative path to the class file
     * @return true if the class should be processed, false if excluded
     */
    boolean shouldProcessClassFile(String relativeClassPath) {
        if (excludes == null || excludes.isEmpty()) {
            return true;
        }

        return !isExcluded(relativeClassPath);
    }

    /**
     * Checks if a path matches any of the exclusion patterns.
     *
     * @param path The path to check
     * @return true if the path is excluded, false otherwise
     */
    private boolean isExcluded(String path) {
        for (String excludePattern : excludes) {
            if (SelectorUtils.matchPath(excludePattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the relative path from a source root to a file.
     *
     * @param file The file
     * @param sourceRoot The source root
     * @return The relative path with forward slashes
     */
    String getRelativePath(File file, File sourceRoot) {
        try {
            Path sourcePath = sourceRoot.toPath().toAbsolutePath();
            Path filePath = file.toPath().toAbsolutePath();
            Path relativePath = sourcePath.relativize(filePath);
            return relativePath.toString().replace('\\', '/');
        } catch (Exception e) {
            // Fallback to simple string manipulation
            String sourceRootPath = sourceRoot.getAbsolutePath();
            String filePath = file.getAbsolutePath();
            if (filePath.startsWith(sourceRootPath)) {
                String relative = filePath.substring(sourceRootPath.length());
                if (relative.startsWith(File.separator)) {
                    relative = relative.substring(1);
                }
                return relative.replace('\\', '/');
            }
            return file.getName();
        }
    }
}
