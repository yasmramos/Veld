import io.github.yasmramos.veld.weaver.FieldInjectorWeaver;
import java.nio.file.Path;
import java.nio.file.Paths;

public class run_weaver {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java run_weaver <classes_directory>");
            return;
        }
        Path classesDir = Paths.get(args[0]);
        System.out.println("Weaving classes in: " + classesDir);
        FieldInjectorWeaver weaver = new FieldInjectorWeaver();
        var results = weaver.weaveDirectory(classesDir);
        System.out.println("Weaving complete. Results: " + results.size());
        for (var r : results) {
            if (r.modified()) {
                System.out.println("  Modified: " + r.className());
            }
        }
    }
}
