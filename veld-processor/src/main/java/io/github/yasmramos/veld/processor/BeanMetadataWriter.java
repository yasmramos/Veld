package io.github.yasmramos.veld.processor;


import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Writes bean metadata to JSON files in META-INF/veld/.
 * 
 * This class serializes bean metadata for each module, enabling cross-module
 * bean discovery by dependent modules during their compilation.
 */
public final class BeanMetadataWriter {

    private static final String METADATA_DIR = "META-INF/veld/";
    private static final String METADATA_SUFFIX = "-beans.json";

    public BeanMetadataWriter() {
    }

    /**
     * Writes metadata for all beans discovered during processing.
     * 
     * @param moduleId unique identifier for this module (usually group:artifact)
     * @param beans list of bean metadata to write
     * @param filer the annotation processor filer for creating resources
     * @param logger logging function for status messages
     * @return number of beans written to metadata
     * @throws IOException if writing fails
     */
    public static int writeMetadata(String moduleId, List<BeanMetadata> beans,
                                     javax.annotation.processing.Filer filer,
                                     java.util.function.Consumer<String> logger) throws IOException {
        if (beans.isEmpty()) {
            logger.accept("[Veld] No beans to export for module: " + moduleId);
            return 0;
        }

        String filename = METADATA_DIR + sanitizeModuleId(moduleId) + METADATA_SUFFIX;
        
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.moduleId = moduleId;
        metadata.generatedAt = System.currentTimeMillis();
        metadata.schemaVersion = "1.0";
        metadata.beans = new ArrayList<>();

        for (BeanMetadata bean : beans) {
            BeanMetadataEntry entry = new BeanMetadataEntry();
            entry.name = bean.getBeanName();
            entry.type = bean.getBeanType();
            entry.factoryClass = bean.getFactoryClassName();
            entry.scope = bean.getScope() != null ? bean.getScope().name() : "SINGLETON";
            entry.qualifier = bean.getQualifier();
            entry.isPrimary = bean.isPrimary();
            entry.dependencies = bean.getDependencies();
            metadata.beans.add(entry);
        }

        String json = toJson(metadata);

        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", filename);
        try (Writer writer = file.openWriter()) {
            writer.write(json);
        }

        logger.accept("[Veld] Exported " + beans.size() + " bean(s) to META-INF/veld/" + 
                     sanitizeModuleId(moduleId) + METADATA_SUFFIX);

        return beans.size();
    }

    /**
     * Sanitizes module ID for use in filename.
     * Replaces characters that are invalid in file paths.
     */
    private static String sanitizeModuleId(String moduleId) {
        return moduleId.replace(':', '_').replace('.', '_').replace('-', '_');
    }

    /**
     * Converts metadata to JSON string.
     * Uses simple string manipulation to avoid JSON library dependency.
     */
    private static String toJson(ModuleMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"moduleId\": \"").append(escapeJson(metadata.moduleId)).append("\",\n");
        sb.append("  \"generatedAt\": ").append(metadata.generatedAt).append(",\n");
        sb.append("  \"schemaVersion\": \"").append(escapeJson(metadata.schemaVersion)).append("\",\n");
        sb.append("  \"beans\": [\n");

        for (int i = 0; i < metadata.beans.size(); i++) {
            BeanMetadataEntry bean = metadata.beans.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escapeJson(bean.name)).append("\",\n");
            sb.append("      \"type\": \"").append(escapeJson(bean.type)).append("\",\n");
            sb.append("      \"factoryClass\": \"").append(escapeJson(bean.factoryClass)).append("\",\n");
            sb.append("      \"scope\": \"").append(bean.scope).append("\",\n");
            sb.append("      \"qualifier\": ").append(bean.qualifier != null ? 
                "\"" + escapeJson(bean.qualifier) + "\"" : "null").append(",\n");
            sb.append("      \"isPrimary\": ").append(bean.isPrimary).append(",\n");
            sb.append("      \"dependencies\": [");
            
            for (int j = 0; j < bean.dependencies.size(); j++) {
                sb.append("\"").append(escapeJson(bean.dependencies.get(j))).append("\"");
                if (j < bean.dependencies.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]\n");
            sb.append("    }");
            if (i < metadata.beans.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Escapes special characters for JSON string values.
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Internal class representing the module metadata structure.
     */
    private static class ModuleMetadata {
        String moduleId;
        long generatedAt;
        String schemaVersion;
        List<BeanMetadataEntry> beans;
    }

    /**
     * Internal class representing a single bean entry in metadata.
     */
    private static class BeanMetadataEntry {
        String name;
        String type;
        String factoryClass;
        String scope;
        String qualifier;
        boolean isPrimary;
        List<String> dependencies;
    }
}
