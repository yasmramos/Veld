public class Test {
    public static void main(String[] args) {
        // Simulated generated code
        int eventId = 123456789;
        String eventTypeName = "io.github.yasmramos.veld.benchmark.features.event.SimpleEvent";
        String methodName = "onSimpleEvent";
        
        String registration = "            bus.registerEventHandler("
                + eventId + ", "
                + eventTypeName + ".class, "
                + "(" + eventTypeName + " event) -> typed." + methodName + "(event));";
        
        System.out.println("Generated code:");
        System.out.println(registration);
    }
}
