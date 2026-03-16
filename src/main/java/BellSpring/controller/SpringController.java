package BellSpring.controller;

import BellSpring.model.Order;
import BellSpring.service.KafkaProducer;
import BellSpring.service.OrderService;
import BellSpring.service.ProductService;
import BellSpring.service.SessionService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import BellSpring.service.DelayService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SpringController {

    private final KafkaProducer kafkaProducer;
    private final SessionService sessionService;
    private final OrderService orderService;
    private final DelayService delayService;

    @PostMapping("/post-message")
    @Timed(value = "api.post.message",
            description = "Time to post message to Kafka")
    public ResponseEntity<String> calculateSquare(@RequestBody Map<String, String> request,
                                                  HttpServletRequest httpRequest) {
        String msg_id = request.get("msg_id");
        long unixtimestampMs = System.currentTimeMillis();
        String unixtimestamp = String.valueOf(unixtimestampMs / 1000);
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        return kafkaProducer.sendToKafka(msg_id, unixtimestamp, method, path);
    }

    @GetMapping("/session/create")
    @Timed(value = "api.session.create",
            description = "Time to create session")
    public ResponseEntity<Map<String, String>> createSession() {
        long start = System.currentTimeMillis();  // +1 строка
        String sessionId = UUID.randomUUID().toString();
        sessionService.createSession(sessionId);

        delayService.applyDelay("session.create",
                System.currentTimeMillis() - start);  // ИЗМЕНЕНО
        return ResponseEntity.ok(Map.of("session_id", sessionId));
    }

    @GetMapping("/order/getProducts")
    @Timed(value = "api.order.products",
            description = "Time to get products")
    public ResponseEntity<?> getProducts(@RequestHeader("Session-ID") String sessionId) {
        long start = System.currentTimeMillis();  // +1 строка
        if (!sessionService.isValidSession(sessionId)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid session");
        }
        Map<String, Integer> products = ProductService.getAllProducts();

        delayService.applyDelay("order.getProducts",
                System.currentTimeMillis() - start);  // ИЗМЕНЕНО
        return ResponseEntity.ok(products);
    }

    @PostMapping("/order/create")
    @Timed(value = "api.order.create",
            description = "Time to create order")
    public ResponseEntity<?> createOrder(@RequestHeader("Session-ID") String sessionId,
                                         @RequestBody Map<String, Object> request) {
        long start = System.currentTimeMillis();  // +1 строка
        if (!sessionService.isValidSession(sessionId)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid session");
        }
        String productName = (String) request.get("product_name");
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        Order order = orderService.createOrder(sessionId, productName, quantity);

        delayService.applyDelay("order.create",
                System.currentTimeMillis() - start);  // ИЗМЕНЕНО
        return ResponseEntity.ok(Map.of("order_id", order.getId()));
    }

    @GetMapping("/order/getOrder")
    @Timed(value = "api.order.get",
            description = "Time to get order")
    public ResponseEntity<?> getOrder(@RequestParam Long order_id,
                                      @RequestParam String product_name,
                                      @RequestHeader("Session-ID") String sessionId) {
        long start = System.currentTimeMillis();  // +1 строка
        try {
            if (!sessionService.isValidSession(sessionId)) {
                return ResponseEntity.status(401).body("Unauthorized: Invalid session");
            }

            Order order = orderService.getOrderByIdWithProductValidation(order_id, product_name);

            delayService.applyDelay("order.getOrder",
                    System.currentTimeMillis() - start);  // ИЗМЕНЕНО
            return ResponseEntity.ok(Map.of(
                    "order_id", order.getId(),
                    "product_name", order.getProductName(),
                    "quantity", order.getQuantity(),
                    "total_price", order.getTotalPrice()
            ));
        } catch (IllegalArgumentException e) {
            delayService.applyDelay("order.getOrder.error",
                    System.currentTimeMillis() - start);  // ИЗМЕНЕНО
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            delayService.applyDelay("order.getOrder.error",
                    System.currentTimeMillis() - start);  // ИЗМЕНЕНО
            return ResponseEntity.internalServerError().body("Error retrieving order");
        }
    }

    @DeleteMapping("/session/delete")
    @Timed(value = "api.session.delete",
            description = "Time to delete session")
    public ResponseEntity<?> deleteSession(@RequestHeader("Session-ID") String sessionId) {
        long start = System.currentTimeMillis();  // +1 строка
        if (!sessionService.isValidSession(sessionId)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid session");
        }
        boolean deleted = sessionService.deleteSession(sessionId);

        delayService.applyDelay("session.delete",
                System.currentTimeMillis() - start);  // ИЗМЕНЕНО

        if (deleted) {
            return ResponseEntity.ok("Session deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Session not found");
        }
    }

    @GetMapping("/order/Check")
    @Timed(value = "api.session.check",
            description = "Time to check session")
    public ResponseEntity<?> checkSession(@RequestParam String session_id) {
        long start = System.currentTimeMillis();  // +1 строка
        boolean isValid = sessionService.isValidSession(session_id);

        delayService.applyDelay("session.check",
                System.currentTimeMillis() - start);  // ИЗМЕНЕНО

        return ResponseEntity.ok(Map.of(
                "session_id", session_id,
                "is_valid", isValid,
                "message", isValid ? "Session is active" : "Session is invalid or deleted"
        ));
    }
}






























