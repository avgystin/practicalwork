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
        // Используем Mono.fromCallable для обертки синхронного/блокирующего кода
        // Выполнится в отдельном потоке при подписке, не блокируя event-loop
        return Mono.fromCallable(() -> {
            // Синхронное извлечение данных из запроса
            String msg_id = request.get("msg_id");
            long unixtimestampMs = System.currentTimeMillis();
            String unixtimestamp = String.valueOf(unixtimestampMs / 1000);
            String method = "POST";
            String path = "/post-message";

            // Блокирующий вызов в Kafka - выполняется в отдельном потоке благодаря fromCallable
            return kafkaProducer.sendToKafka(msg_id, unixtimestamp, method, path);
        });
        // Результат: Mono<ResponseEntity<String>> - реактивная обертка над строковым ответом
    }

    @GetMapping("/session/create")
    public Mono<ResponseEntity<Map<String, String>>> createSession() {
        // Реактивная цепочка: задержка → создание сессии → преобразование в ответ
        // .then() игнорирует результат delay (Mono<Void>) и продолжает цепочку
        return delayService.applyDelay("session.create")
                .then(sessionService.createSession()) // Асинхронное создание сессии
                .map(sessionId -> ResponseEntity.ok(Map.of("session_id", sessionId)));
        // map преобразует String sessionId в ResponseEntity
    }

    @GetMapping("/order/getProducts")
    public Mono<ResponseEntity<?>> getProducts(@RequestHeader("Session-ID") String sessionId) {
        // Начинаем с проверки сессии - основа безопасности эндпоинтов
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> { // flatMap т.к. возвращаем новый Mono в зависимости от isValid
                    if (!isValid) {
                        // Ранний возврат ошибки 401 - прерывание нормального потока
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    // Сессия валидна - продолжаем выполнение
                    return delayService.applyDelay("order.getProducts")
                            .then(productService.getAllProducts() // Асинхронное получение продуктов
                                    // Явное указание типа для map (дженерики в реактивных цепях)
                                    .<ResponseEntity<?>>map(products -> ResponseEntity.ok(products))
                                    // Глобальная обработка ошибок для этого потока
                                    .onErrorResume(e -> Mono.just(
                                            ResponseEntity.status(500).body("Error retrieving products")
                                    )));
                });
        // Итог: цепочка возвращает либо продукты (200), либо ошибку (401/500)
    }

    @PostMapping("/order/create")
    public Mono<ResponseEntity<?>> createOrder(@RequestHeader("Session-ID") String sessionId,
                                               @RequestBody Map<String, Object> request) {
        // 1. Начинаем реактивную цепочку: проверяем валидность сессии
        //    sessionService.isValidSession() возвращает Mono<Boolean> - асинхронный результат
        return sessionService.isValidSession(sessionId)
                // 2. flatMap используется потому что следующий шаг тоже возвращает Mono
                //    Получаем Boolean isValid из предыдущего Mono и решаем что делать дальше
                .flatMap(isValid -> {
                    // 3. Если сессия невалидна - немедленно прерываем цепочку
                    //    Mono.just() создает новый Mono с готовым значением (401 ошибка)
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }

                    // 4. Извлекаем параметры СЕЙЧАС, так как мы уже знаем что сессия валидна
                    //    Эти операции выполняются синхронно в том же потоке
                    String productName = (String) request.get("product_name");
                    Integer quantity = Integer.valueOf(request.get("quantity").toString());

                    // 5. Применяем задержку (асинхронная операция)
                    //    delayService.applyDelay() возвращает Mono<Void> - сигнал "готово"
                    //    .then() говорит: "после завершения delay, выполни следующее"
                    return delayService.applyDelay("order.create")
                            // 6. Вызываем создание заказа
                            //    orderService.createOrder() возвращает Mono<Order> - асинхронная операция
                            //    Весь блокирующий код (работа с БД) уже изолирован внутри OrderService
                            .then(orderService.createOrder(sessionId, productName, quantity))
                            // 7. Преобразуем Order в ResponseEntity
                            //    <ResponseEntity<?>> явное указание типа нужно для компилятора
                            //    Выполнится только если createOrder() успешно завершится
                            .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of("order_id", order.getId())))
                            // 8. Обработка ошибок в реактивном стиле
                            //    onErrorResume перехватывает исключения и возвращает альтернативный Mono
                            //    Не прерывает выполнение приложения, просто заменяет результат в цепочке
                            .onErrorResume(IllegalArgumentException.class,
                                    e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
                });
        // 9. Вся цепочка возвращается как Mono<ResponseEntity<?>>
        //    Spring WebFlux подпишется на этот Mono когда придет HTTP запрос
        //    Реальный код выполнится только в момент подписки (lazy execution)
    }

    @GetMapping("/order/getOrder")
    public Mono<ResponseEntity<?>> getOrder(@RequestParam Long order_id,
                                            @RequestParam String product_name,
                                            @RequestHeader("Session-ID") String sessionId) {
        // Стандартный паттерн: проверка сессии → задержка → бизнес-логика
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    // Сессия валидна - применяем задержку и получаем заказ
                    return delayService.applyDelay("order.getOrder")
                            .then(orderService.getOrderById(order_id, product_name) // Асинхронный поиск заказа
                                    // Преобразование Order в структурированный JSON ответ
                                    .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                                            "order_id", order.getId(),
                                            "product_name", order.getProductName(),
                                            "quantity", order.getQuantity(),
                                            "total_price", order.getTotalPrice()
                                    )))
                                    // Иерархическая обработка ошибок: сначала конкретные, потом общие
                                    .onErrorResume(IllegalArgumentException.class,
                                            e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())))
                                    .onErrorResume(Exception.class,
                                            e -> Mono.just(ResponseEntity.internalServerError().body("Error retrieving order")))
                            );
                });
        // Возможные исходы: успех (200), невалидная сессия (401), неверные параметры (400), серверная ошибка (500)
    }

    @DeleteMapping("/session/delete")
    public Mono<ResponseEntity<?>> deleteSession(@RequestHeader("Session-ID") String sessionId) {
        // Удаление сессии также требует предварительной валидации
        return sessionService.isValidSession(sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.status(401).body("Unauthorized: Invalid session"));
                    }
                    // Сессия существует и валидна - можно удалять
                    return delayService.applyDelay("session.delete")
                            .then(sessionService.deleteSession(sessionId)) // Асинхронное удаление
                            .map(deleted -> { // Обработка результата Boolean
                                if (deleted) {
                                    return ResponseEntity.ok("Session deleted successfully");
                                } else {
                                    // Сессия была валидной, но не найдена для удаления
                                    return ResponseEntity.status(404).body("Session not found");
                                }
                            });
                });
        // Итог: 200 (удалено), 401 (невалидная), 404 (не найдена)
    }

    @GetMapping("/order/Check")
    public Mono<ResponseEntity<?>> checkSession(@RequestParam String session_id) {
        // Упрощенный эндпоинт без проверки заголовков - только параметр запроса
        // Полезно для внешней проверки статуса сессии
        return delayService.applyDelay("session.check")
                .then(sessionService.isValidSession(session_id)) // Проверка сессии по ID
                .map(isValid -> ResponseEntity.ok(Map.of( // Структурированный ответ
                        "session_id", session_id,
                        "is_valid", isValid,
                        "message", isValid ? "Session is active" : "Session is invalid or deleted"
                )));
        // Всегда возвращает 200, но с информацией о статусе сессии в теле
    }
}

// КЛЮЧЕВЫЕ РЕАКТИВНЫЕ ПАТТЕРНЫ В ЭТОМ КОНТРОЛЛЕРЕ:
//
// 1. ЕДИНООБРАЗНАЯ СТРУКТУРА:
//    Все эндпоинты возвращают Mono<ResponseEntity<?>> для согласованности
//
// 2. БЕЗОПАСНОСТЬ ЧЕРЕЗ СЕССИИ:
//    Большинство эндпоинтов начинают с sessionService.isValidSession()
//
// 3. ЗАДЕРЖКИ ДЛЯ ЭМУЛЯЦИИ РЕАЛЬНЫХ УСЛОВИЙ:
//    delayService.applyDelay() добавляет реалистичные задержки обработки
//
// 4. ОБРАБОТКА ОШИБОК НА УРОВНЕ ПОТОКА:
//    onErrorResume() перехватывает исключения и преобразует в HTTP-ответы
//
// 5. ИЗОЛЯЦИЯ БЛОКИРУЮЩИХ ОПЕРАЦИЙ:
//    Блокирующий код (Kafka, БД) обернут в fromCallable или изолирован в сервисах
//
// 6. LAZY EXECUTION:
//    Никакой код не выполняется до подписки Spring WebFlux на возвращенный Mono
//
// 7. NON-BLOCKING ВЕЗДЕ ГДЕ ВОЗМОЖНО:
//    Event-loop поток никогда не блокируется на I/O операциях






























