package BellSpring.controller;

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
        String msg_id = request.get("msg_id");
        long unixtimestampMs = System.currentTimeMillis();
        String unixtimestamp = String.valueOf(unixtimestampMs / 1000);
        String method = "POST";
        String path = "/post-message";

        // Вызываем напрямую реактивный метод
        return kafkaProducer.sendToKafka(msg_id, unixtimestamp, method, path)
                .map(result -> {
                    if (result.startsWith("OK")) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(500).body(result);
                    }
                })
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(500).body("Internal server error: " + e.getMessage())
                ));
    }

    @GetMapping("/session/create")
    public Mono<ResponseEntity<Map<String, String>>> createSession() {
        return delayService.applyDelay("session.create")
                .then(sessionService.createSession())
                .map(sessionId -> ResponseEntity.ok(Map.of("session_id", sessionId)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", "Internal server error"))));
    }

    @GetMapping("/order/getProducts")
    public Mono<ResponseEntity<?>> getProducts(@RequestHeader("Session-ID") String sessionId) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    return delayService.applyDelay("order.getProducts")
                            .then(productService.getAllProducts()
                                    .<ResponseEntity<?>>map(products -> ResponseEntity.ok(products))
                            );
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Internal server error")));
    }

    @PostMapping("/order/create")
    public Mono<ResponseEntity<?>> createOrder(@RequestHeader("Session-ID") String sessionId,
                                               @RequestBody Map<String, Object> request) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }

                    String productName = (String) request.get("product_name");
                    Integer quantity = Integer.valueOf(request.get("quantity").toString());

                    return delayService.applyDelay("order.create")
                            .then(orderService.createOrder(sessionId, productName, quantity))
                            .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of("order_id", order.getId())))
                            .onErrorResume(IllegalArgumentException.class,
                                    e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Internal server error")));
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
                            .then(orderService.getOrderById(order_id, product_name))
                            .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                                    "order_id", order.getId(),
                                    "product_name", order.getProductName(),
                                    "quantity", order.getQuantity(),
                                    "total_price", order.getTotalPrice()
                            )))
                            .onErrorResume(IllegalArgumentException.class,
                                    e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error retrieving order")));
    }

    @DeleteMapping("/session/delete")
    public Mono<ResponseEntity<String>> deleteSession(@RequestHeader("Session-ID") String sessionId) {
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    return delayService.applyDelay("session.delete")
                            .then(sessionService.deleteSession(sessionId))
                            .map(deleted -> deleted ? "Session deleted successfully" : "Session not found")
                            .map(ResponseEntity::ok);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Internal server error")));
    }

    @GetMapping("/order/Check")
    public Mono<ResponseEntity<Map<String, Object>>> checkSession(@RequestParam String session_id) {
        return delayService.applyDelay("session.check")
                .then(sessionService.isValidSession(session_id))
                .map(isValid -> Map.<String, Object>of(
                        "session_id", session_id,
                        "is_valid", isValid,
                        "message", isValid ? "Session is active" : "Session is invalid or deleted"
                ))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(
                        Map.<String, Object>of(
                                "session_id", session_id,
                                "error", "Internal server error"
                        )
                )));
    }
}
// КЛЮЧЕВЫЕ РЕАКТИВНЫЕ ПАТТЕРНЫ В ЭТОМ КОНТРОЛЛЕРЕ:
//
// 1. ЕДИНООБРАЗНАЯ СТРУКТУРА:
//    Все эндпоинты возвращают Mono<ResponseEntity<?>> для согласованности
//    Специфичные эндпоинты имеют конкретные типы (Mono<ResponseEntity<String>>, Mono<ResponseEntity<Map<String, Object>>>)
//
// 2. БЕЗОПАСНОСТЬ ЧЕРЕЗ СЕССИИ:
//    Большинство эндпоинтов начинают с sessionService.isValidSession()
//    Валидация сессии - первый шаг в реактивной цепочке, прерывает выполнение при невалидной сессии
//
// 3. ЗАДЕРЖКИ ДЛЯ ЭМУЛЯЦИИ РЕАЛЬНЫХ УСЛОВИЙ:
//    delayService.applyDelay() добавляет реалистичные задержки обработки
//    Конфигурируемые задержки для разных эндпоинтов через application.yml
//
// 4. ОБРАБОТКА ОШИБОК НА УРОВНЕ ПОТОКА:
//    onErrorResume() перехватывает исключения и преобразует в HTTP-ответы
//    Иерархическая обработка: IllegalArgumentException → 400, Exception → 500
//    Graceful degradation вместо падения приложения
//
// 5. ИЗОЛЯЦИЯ БЛОКИРУЮЩИХ ОПЕРАЦИЙ:
//    Блокирующий код (Kafka, БД) обернут в fromCallable или изолирован в сервисах
//    subscribeOn(Schedulers.boundedElastic()) для выполнения блокирующих операций в отдельных потоках
//    Event-loop потоки никогда не блокируются на I/O
//
// 6. LAZY EXECUTION:
//    Никакой код не выполняется до подписки Spring WebFlux на возвращенный Mono
//    Все операции откладываются до фактического HTTP запроса
//    Эффективное использование ресурсов - нет предварительных вычислений
//
// 7. NON-BLOCKING ВЕЗДЕ ГДЕ ВОЗМОЖНО:
//    Event-loop поток никогда не блокируется на I/O операциях
//    Асинхронные вызовы между сервисами через Mono.flatMap()
//    Реактивные цепочки вместо синхронных вызовов
//
// 8. РЕАКТИВНЫЕ ЦЕПОЧКИ (REACTIVE CHAINING):
//    Использование .then() для последовательного выполнения асинхронных операций
//    flatMap() для преобразования результатов с сохранением реактивности
//    map() для синхронных преобразований внутри реактивного контекста
//
// 9. ТИПОБЕЗОПАСНОСТЬ И КОМПОЗИЦИЯ:
//     Явное указание типов через <ResponseEntity<?>>map() для сложных преобразований
//     Композиция сервисов через flatMap() для создания сложных бизнес-процессов
//     Четкое разделение ответственности между контроллером и сервисами
//
// 10. BACKPRESSURE (ОБРАТНОЕ ДАВЛЕНИЕ):
//     Spring WebFlux автоматически управляет backpressure через Reactive Streams
//     Контроллер работает в ритме, заданном клиентом/сетью
//     Предотвращение перегрузки системы при высокой нагрузке
//
// 11. МОНИТОРИНГ И ОТСЛЕЖИВАЕМОСТЬ:
//     Структурированные ответы об ошибках для клиентов
//     Готовность к интеграции с distributed tracing (Micrometer, OpenTelemetry)






























