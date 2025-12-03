package BellSpring.service;

import BellSpring.model.Order;
import BellSpring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    /**
     * Создание заказа с использованием реактивного программирования
     * @param sessionId ID сессии пользователя
     * @param productName название продукта
     * @param quantity количество
     * @return Mono<Order> - реактивный результат создания заказа
     */
    public Mono<Order> createOrder(String sessionId, String productName, Integer quantity) {
        // 1. Реактивно получаем цену продукта
        return productService.getProductPrice(productName)
                // 2. После получения цены создаем заказ
                .flatMap(unitPrice -> Mono.fromCallable(() -> {
                    // Рассчитываем общую стоимость
                    Integer totalPrice = unitPrice * quantity;

                    // Создаем объект заказа
                    Order order = new Order();
                    order.setSessionId(sessionId);
                    order.setProductName(productName);
                    order.setQuantity(quantity);
                    order.setUnitPrice(unitPrice);
                    order.setTotalPrice(totalPrice);

                    // Сохраняем в БД (блокирующая операция)
                    return orderRepository.save(order);
                    // 3. Выполняем блокирующую операцию в отдельном пуле потоков
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Получение заказа по ID с валидацией названия продукта
     * @param orderId ID заказа
     * @param expectedProductName ожидаемое название продукта для валидации
     * @return Mono<Order> - реактивный результат поиска заказа
     */
    public Mono<Order> getOrderById(Long orderId, String expectedProductName) {
        return Mono.fromCallable(() -> {
            // Поиск заказа в БД (блокирующая операция)
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                throw new IllegalArgumentException("Order not found with id: " + orderId);
            }

            Order order = orderOpt.get();

            // Валидация: проверяем совпадает ли название продукта
            if (!order.getProductName().equals(expectedProductName)) {
                throw new IllegalArgumentException(
                        "Product name validation failed. Expected: " + expectedProductName +
                                ", but got: " + order.getProductName()
                );
            }
            return order;
            // Выполняем в отдельном пуле потоков чтобы не блокировать event loop
        }).subscribeOn(Schedulers.boundedElastic());
    }
}