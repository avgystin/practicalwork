package BellSpring.controller;

import BellSpring.model.Order;
import BellSpring.service.KafkaProducer;
import BellSpring.service.OrderService;
import BellSpring.service.ProductService;
import BellSpring.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SpringController {

    private final KafkaProducer kafkaProducer;
    private final SessionService sessionService;
    private final OrderService orderService;

    @PostMapping("/post-message")
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
    public ResponseEntity<Map<String, String>> createSession() {
        // Создаем новую сессию
        String sessionId = UUID.randomUUID().toString();
        sessionService.createSession(sessionId);

        // Возвращаем UUID сессии
        return ResponseEntity.ok(Map.of("session_id", sessionId));
    }

    @GetMapping("/order/getProducts")
    public ResponseEntity<?> getProducts(@RequestHeader("Session-ID") String sessionId) {
        // Проверяем валидность сессии
        if (!sessionService.isValidSession(sessionId)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid session");
        }
        // Возвращаем список продуктов
        Map<String, Integer> products = ProductService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @PostMapping("/order/create")
    public ResponseEntity<?> createOrder(@RequestHeader("Session-ID") String sessionId,
                                             @RequestBody Map<String, Object> request) {
        // Проверяем валидность сессии
        if (!sessionService.isValidSession(sessionId)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid session");
        }
        String productName = (String) request.get("product_name");
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        // Создаем заказ
        Order order = orderService.createOrder(sessionId, productName, quantity);

        // Возвращаем ID заказа
        return ResponseEntity.ok(Map.of(
                "order_id", order.getId()
        ));
    }

    @GetMapping("/order/getOrder")
    public ResponseEntity<?> getOrder(@RequestParam Long order_id,
                                      @RequestParam String product_name,
                                      @RequestHeader("Session-ID") String sessionId) {
        try {
            // Проверяем валидность сессии
            if (!sessionService.isValidSession(sessionId)) {
                return ResponseEntity.status(401).body("Unauthorized: Invalid session");
            }

            // Получаем заказ с валидацией по названию продукта
            Order order = orderService.getOrderByIdWithProductValidation(order_id, product_name);

            return ResponseEntity.ok(Map.of(
                    "order_id", order.getId(),
                    "product_name", order.getProductName(),
                    "quantity", order.getQuantity(),
                    "total_price", order.getTotalPrice()
            ));
        } catch (IllegalArgumentException e) {
            // Перехватываем исключение и возвращаем сообщение об ошибке
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving order");
        }
    }

    @DeleteMapping("/session/delete")
    public ResponseEntity<?> deleteSession(@RequestHeader("Session-ID") String sessionId) {

        // Проверяем валидность сессии
        if (!sessionService.isValidSession(sessionId)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid session");
        }
        boolean deleted = sessionService.deleteSession(sessionId);

        if (deleted) {
            return ResponseEntity.ok("Session deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Session not found");
        }
    }

    @GetMapping("/order/Check")
    public ResponseEntity<?> checkSession(@RequestParam String session_id) {

        boolean isValid = sessionService.isValidSession(session_id);

        return ResponseEntity.ok(Map.of(
                "session_id", session_id,
                "is_valid", isValid,
                "message", isValid ? "Session is active" : "Session is invalid or deleted"
        ));
    }
}






























