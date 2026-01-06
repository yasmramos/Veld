package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;

/**
 * Example service using Veld annotations.
 * Demonstrates Veld's native annotation support for dependency injection.
 * 
 * Note: @Component provides the same functionality as @jakarta.inject.Singleton + @Named.
 * Veld recognizes Veld annotations natively.
 */
@Component("orderService")
public class OrderService {
    
    private LogService logService;
    private PaymentService paymentService;
    private IUserRepository userRepository;
    
    /**
     * Constructor injection using Veld @Inject
     */
    @Inject
    public OrderService(LogService logService) {
        this.logService = logService;
        System.out.println("[OrderService] Created with Veld @Inject constructor");
    }
    
    /**
     * Method injection using Veld @Inject
     */
    @Inject
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
        System.out.println("[OrderService] PaymentService injected via Veld @Inject method");
    }
    
    /**
     * Another method injection demonstrating interface-based injection
     */
    @Inject
    public void setUserRepository(IUserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("[OrderService] IUserRepository injected -> " + 
            userRepository.getClass().getSimpleName());
    }
    
    @PostConstruct
    public void init() {
        logService.log("OrderService initialized with Jakarta Inject annotations");
    }
    
    public String createOrder(Long userId, String productName, double price) {
        // Validate user exists
        var user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return "ERROR: User not found";
        }
        
        // Validate payment
        if (!paymentService.validatePayment(price)) {
            return "ERROR: Payment validation failed";
        }
        
        String orderId = "ORD-" + System.currentTimeMillis();
        logService.log("Order created: " + orderId + " for user: " + user.get());
        
        // Process payment
        paymentService.processPayment(orderId, price);
        
        return orderId;
    }
    
    public void cancelOrder(String orderId) {
        logService.log("Order cancelled: " + orderId);
        System.out.println("[OrderService] Order " + orderId + " has been cancelled");
    }
}
