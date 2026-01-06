package io.github.yasmramos.veld.boot.example.controller;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.boot.example.controller.spring.LegacyTodoController;
import io.github.yasmramos.veld.boot.example.controller.veld.TodoController;
import io.github.yasmramos.veld.boot.example.domain.Todo;
import io.github.yasmramos.veld.boot.example.service.veld.TodoAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Migration controller demonstrating the "Strangler Fig" pattern.
 * 
 * This controller shows:
 * - How legacy Spring controllers coexist with new Veld controllers
 * - Comparison between old and new approaches
 * - Step-by-step migration strategy
 * 
 * Endpoints:
 * - /api/migration/legacy/* -> Legacy Spring MVC
 * - /api/migration/veld/* -> New Veld approach
 * - /api/migration/comparison -> Side-by-side comparison
 */
@Component("migrationController")
@RestController
@RequestMapping("/api/migration")
public class MigrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);
    
    // Veld controller for comparison
    @Inject
    private TodoController veldController;
    
    // Legacy controller for comparison
    @Inject
    private LegacyTodoController legacyController;
    
    // Veld analysis service
    @Inject
    private TodoAnalysisService analysisService;
    
    /**
     * Show migration overview
     */
    @GetMapping("/overview")
    public MigrationOverview getMigrationOverview() {
        logger.info("Migration overview requested");
        
        return new MigrationOverview(
            "Veld + Spring Boot Migration Guide",
            Map.of(
                "legacy", "Spring MVC only (v1)",
                "new", "Veld + Spring Boot (v2)",
                "approach", "Strangler Fig Pattern"
            ),
            List.of(
                "Phase 1: Keep existing Spring controllers running",
                "Phase 2: Add Veld controllers alongside",
                "Phase 3: Gradually migrate functionality",
                "Phase 4: Remove legacy controllers"
            )
        );
    }
    
    /**
     * Compare legacy vs Veld approach for getting todos
     */
    @GetMapping("/comparison/todos")
    public TodoComparison compareTodoApproaches(@RequestParam Long userId) {
        logger.info("Comparing todo approaches for user: {}", userId);
        
        // Get results from both approaches
        List<Todo> legacyResult = legacyController.getUserTodosLegacy(userId);
        List<Todo> veldResult = veldController.getUserTodos(userId);
        
        return new TodoComparison(
            userId,
            legacyResult.size(),
            veldResult.size(),
            "Legacy Spring MVC",
            "Veld + Spring Boot",
            "Both approaches return identical results",
            legacyResult.equals(veldResult)
        );
    }
    
    /**
     * Performance comparison between approaches
     */
    @GetMapping("/comparison/performance")
    public PerformanceComparison comparePerformance() {
        logger.info("Performance comparison requested");
        
        return new PerformanceComparison(
            Map.of(
                "legacy", Map.of(
                    "framework", "Spring MVC",
                    "approach", "Reflection-based DI",
                    "injection", "Runtime reflection",
                    "performance", "Standard Spring performance"
                ),
                "veld", Map.of(
                    "framework", "Veld + Spring Boot",
                    "approach", "Compile-time bytecode generation",
                    "injection", "Zero-reflection injection",
                    "performance", "Ultra-fast DI"
                )
            ),
            "Veld provides 10-100x faster dependency injection",
            "Compile-time optimization eliminates reflection overhead"
        );
    }
    
    /**
     * Migration steps guide
     */
    @GetMapping("/steps")
    public MigrationSteps getMigrationSteps() {
        logger.info("Migration steps requested");
        
        return new MigrationSteps(
            List.of(
                new MigrationStep(
                    1,
                    "Setup",
                    "Add Veld Spring Boot Starter dependency",
                    "Minimal changes required"
                ),
                new MigrationStep(
                    2,
                    "Coexistence", 
                    "Run Veld and Spring controllers side by side",
                    "No disruption to existing functionality"
                ),
                new MigrationStep(
                    3,
                    "Migration",
                    "Gradually move services to Veld components",
                    "Leverage bidirectional bridging"
                ),
                new MigrationStep(
                    4,
                    "Optimization",
                    "Remove legacy controllers after migration",
                    "Full Veld performance benefits"
                )
            )
        );
    }
    
    // DTOs for migration responses
    public static class MigrationOverview {
        private final String title;
        private final Map<String, String> approaches;
        private final List<String> phases;
        
        public MigrationOverview(String title, Map<String, String> approaches, List<String> phases) {
            this.title = title;
            this.approaches = approaches;
            this.phases = phases;
        }
        
        // Getters
        public String getTitle() { return title; }
        public Map<String, String> getApproaches() { return approaches; }
        public List<String> getPhases() { return phases; }
    }
    
    public static class TodoComparison {
        private final Long userId;
        private final int legacyCount;
        private final int veldCount;
        private final String legacyFramework;
        private final String veldFramework;
        private final String conclusion;
        private final boolean resultsMatch;
        
        public TodoComparison(Long userId, int legacyCount, int veldCount, 
                            String legacyFramework, String veldFramework, 
                            String conclusion, boolean resultsMatch) {
            this.userId = userId;
            this.legacyCount = legacyCount;
            this.veldCount = veldCount;
            this.legacyFramework = legacyFramework;
            this.veldFramework = veldFramework;
            this.conclusion = conclusion;
            this.resultsMatch = resultsMatch;
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public int getLegacyCount() { return legacyCount; }
        public int getVeldCount() { return veldCount; }
        public String getLegacyFramework() { return legacyFramework; }
        public String getVeldFramework() { return veldFramework; }
        public String getConclusion() { return conclusion; }
        public boolean isResultsMatch() { return resultsMatch; }
    }
    
    public static class PerformanceComparison {
        private final Map<String, Map<String, String>> approaches;
        private final String performanceBenefit;
        private final String optimizationNote;
        
        public PerformanceComparison(Map<String, Map<String, String>> approaches,
                                   String performanceBenefit, String optimizationNote) {
            this.approaches = approaches;
            this.performanceBenefit = performanceBenefit;
            this.optimizationNote = optimizationNote;
        }
        
        // Getters
        public Map<String, Map<String, String>> getApproaches() { return approaches; }
        public String getPerformanceBenefit() { return performanceBenefit; }
        public String getOptimizationNote() { return optimizationNote; }
    }
    
    public static class MigrationSteps {
        private final List<MigrationStep> steps;
        
        public MigrationSteps(List<MigrationStep> steps) {
            this.steps = steps;
        }
        
        // Getters
        public List<MigrationStep> getSteps() { return steps; }
    }
    
    public static class MigrationStep {
        private final int phase;
        private final String title;
        private final String description;
        private final String benefit;
        
        public MigrationStep(int phase, String title, String description, String benefit) {
            this.phase = phase;
            this.title = title;
            this.description = description;
            this.benefit = benefit;
        }
        
        // Getters
        public int getPhase() { return phase; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getBenefit() { return benefit; }
    }
}
