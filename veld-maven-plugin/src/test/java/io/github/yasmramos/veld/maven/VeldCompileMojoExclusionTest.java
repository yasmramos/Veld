package io.github.yasmramos.veld.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for VeldCompileMojo exclusion functionality
 */
public class VeldCompileMojoExclusionTest {

    private VeldCompileMojo mojo;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        mojo = new VeldCompileMojo();
    }

    @Test
    public void testNoExclusions() throws Exception {
        // Test that when no exclusions are configured, all files are processed
        File testFile = createTestFile("com/example/TestService.java");
        File sourceRoot = tempDir.toFile();
        
        boolean result = mojo.shouldProcessFile(testFile, sourceRoot, ".java");
        
        assertTrue(result, "File should be processed when no exclusions are configured");
    }

    @Test
    public void testExcludeTestFiles() throws Exception {
        // Configure exclusions
        List<String> excludes = Arrays.asList("**/*Test*.class");
        mojo.setExcludes(excludes);
        
        File testFile = createTestFile("com/example/TestService.java");
        File regularFile = createTestFile("com/example/RegularService.java");
        File sourceRoot = tempDir.toFile();
        
        assertFalse(mojo.shouldProcessFile(testFile, sourceRoot, ".java"), 
                   "Test file should be excluded");
        assertTrue(mojo.shouldProcessFile(regularFile, sourceRoot, ".java"), 
                  "Regular file should be processed");
    }

    @Test
    public void testExcludePackage() throws Exception {
        // Configure exclusions
        List<String> excludes = Arrays.asList("com/legacy/**");
        mojo.setExcludes(excludes);
        
        File legacyFile = createTestFile("com/legacy/OldService.java");
        File newFile = createTestFile("com/example/NewService.java");
        File sourceRoot = tempDir.toFile();
        
        assertFalse(mojo.shouldProcessFile(legacyFile, sourceRoot, ".java"), 
                   "Legacy file should be excluded");
        assertTrue(mojo.shouldProcessFile(newFile, sourceRoot, ".java"), 
                  "New file should be processed");
    }

    @Test
    public void testExcludeGenerated() throws Exception {
        // Configure exclusions
        List<String> excludes = Arrays.asList("**/generated/**");
        mojo.setExcludes(excludes);
        
        File generatedFile = createTestFile("target/generated/com/example/Generated.java");
        File regularFile = createTestFile("src/main/java/com/example/Service.java");
        File sourceRoot = tempDir.toFile();
        
        assertFalse(mojo.shouldProcessFile(generatedFile, sourceRoot, ".java"), 
                   "Generated file should be excluded");
        assertTrue(mojo.shouldProcessFile(regularFile, sourceRoot, ".java"), 
                  "Regular file should be processed");
    }

    @Test
    public void testMultipleExclusions() throws Exception {
        // Configure multiple exclusions
        List<String> excludes = Arrays.asList(
            "**/*Test*.class", 
            "com/legacy/**", 
            "**/generated/**"
        );
        mojo.setExcludes(excludes);
        
        File testFile = createTestFile("com/example/TestService.java");
        File legacyFile = createTestFile("com/legacy/OldService.java");
        File generatedFile = createTestFile("generated/MyGenerated.java");
        File regularFile = createTestFile("com/example/RegularService.java");
        File sourceRoot = tempDir.toFile();
        
        assertFalse(mojo.shouldProcessFile(testFile, sourceRoot, ".java"), 
                   "Test file should be excluded");
        assertFalse(mojo.shouldProcessFile(legacyFile, sourceRoot, ".java"), 
                   "Legacy file should be excluded");
        assertFalse(mojo.shouldProcessFile(generatedFile, sourceRoot, ".java"), 
                   "Generated file should be excluded");
        assertTrue(mojo.shouldProcessFile(regularFile, sourceRoot, ".java"), 
                  "Regular file should be processed");
    }

    @Test
    public void testClassFileExclusion() throws Exception {
        // Test exclusion during weaving phase
        List<String> excludes = Arrays.asList("com/example/**");
        mojo.setExcludes(excludes);
        
        assertFalse(mojo.shouldProcessClassFile("com/example/TestService.class"), 
                   "Class file should be excluded from weaving");
        assertTrue(mojo.shouldProcessClassFile("com/other/Service.class"), 
                  "Other class file should be processed");
    }

    private File createTestFile(String relativePath) throws Exception {
        File file = new File(tempDir.toFile(), relativePath);
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }
}