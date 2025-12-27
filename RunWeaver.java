import io.github.yasmramos.veld.weaver.VeldClassGenerator;
import io.github.yasmramos.veld.weaver.VeldClassGenerator.ComponentMeta;
import java.nio.file.*;
import java.util.*;

public class RunWeaver {
    public static void main(String[] args) throws Exception {
        Path classesDir = Path.of(args[0]);
        List<ComponentMeta> components = VeldClassGenerator.readMetadata(classesDir);
        System.out.println("Found " + components.size() + " components");
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        byte[] bytecode = generator.generate();
        
        Path veldClass = classesDir.resolve("io/github/yasmramos/veld/Veld.class");
        Files.createDirectories(veldClass.getParent());
        Files.write(veldClass, bytecode);
        System.out.println("Generated: " + veldClass);
    }
}
