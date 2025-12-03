package BellSpring.controller;

import BellSpring.model.Order;
import BellSpring.service.KafkaProducer;
import BellSpring.service.OrderService;
import BellSpring.service.ProductService;
import BellSpring.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import BellSpring.service.DelayService;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SpringController {

    private final KafkaProducer kafkaProducer;
    private final SessionService sessionService;
    private final OrderService orderService;
    private final DelayService delayService;
    private final ProductService productService;

    @PostMapping("/post-message")
    public Mono<ResponseEntity<String>> calculateSquare(@RequestBody Map<String, String> request) {
        return Mono.fromCallable(() -> {
            String msg_id = request.get("msg_id");
            long unixtimestampMs = System.currentTimeMillis();
            String unixtimestamp = String.valueOf(unixtimestampMs / 1000);
            String method = "POST";
            String path = "/post-message";

            return kafkaProducer.sendToKafka(msg_id, unixtimestamp, method, path);
        });
    }

    @GetMapping("/session/create")
    public Mono<ResponseEntity<Map<String, String>>> createSession() {
        return delayService.applyDelay("session.create")
                .then(sessionService.createSession())
                .map(sessionId -> ResponseEntity.ok(Map.of("session_id", sessionId)));
    }

    @GetMapping("/order/getProducts")
    public Mono<ResponseEntity<?>> getProducts(@RequestHeader("Session-ID") String sessionId) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    return delayService.applyDelay("order.getProducts")
                            .then(productService.getAllProducts()  // <-- ИЗМЕНИЛ: getAllProducts() вместо getAllProductsReactive()
                                    .<ResponseEntity<?>>map(products -> ResponseEntity.ok(products))
                                    .onErrorResume(e -> Mono.just(
                                            ResponseEntity.status(500).body("Error retrieving products")
                                    )));
                });
    }

    @PostMapping("/order/create")
    public Mono<ResponseEntity<?>> createOrder(@RequestHeader("Session-ID") String sessionId,
                                               @RequestBody Map<String, Object> request) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    return delayService.applyDelay("order.create")
                            .then(Mono.defer(() -> {
                                String productName = (String) request.get("product_name");
                                Integer quantity = Integer.valueOf(request.get("quantity").toString());

                                return orderService.createOrder(sessionId, productName, quantity)  // <-- ИЗМЕНИЛ: createOrder() вместо createOrderReactive()
                                        .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of("order_id", order.getId())))
                                        .onErrorResume(IllegalArgumentException.class,
                                                e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
                            }));
                });
    }

    @GetMapping("/order/getOrder")
    public Mono<ResponseEntity<?>> getOrder(@RequestParam Long order_id,
                                            @RequestParam String product_name,
                                            @RequestHeader("Session-ID") String sessionId) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    return delayService.applyDelay("order.getOrder")
                            .then(orderService.getOrderById(order_id, product_name)  // <-- ИЗМЕНИЛ: getOrderById() вместо getOrderByIdReactive()
                                    .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                                            "order_id", order.getId(),
                                            "product_name", order.getProductName(),
                                            "quantity", order.getQuantity(),
                                            "total_price", order.getTotalPrice()
                                    )))
                                    .onErrorResume(IllegalArgumentException.class,
                                            e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())))
                                    .onErrorResume(Exception.class,
                                            e -> Mono.just(ResponseEntity.internalServerError().body("Error retrieving order")))
                            );
                });
    }

    @DeleteMapping("/session/delete")
    public Mono<ResponseEntity<?>> deleteSession(@RequestHeader("Session-ID") String sessionId) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    return delayService.applyDelay("session.delete")
                            .then(sessionService.deleteSession(sessionId))
                            .map(deleted -> {
                                if (deleted) {
                                    return ResponseEntity.ok("Session deleted successfully");
                                } else {
                                    return ResponseEntity.status(404).body("Session not found");
                                }
                            });
                });
    }

    @GetMapping("/order/Check")
    public Mono<ResponseEntity<?>> checkSession(@RequestParam String session_id) {
        return delayService.applyDelay("session.check")
                .then(sessionService.isValidSession(session_id))
                .map(isValid -> ResponseEntity.ok(Map.of(
                        "session_id", session_id,
                        "is_valid", isValid,
                        "message", isValid ? "Session is active" : "Session is invalid or deleted"
                )));
    }
}






























