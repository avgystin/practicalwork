package BellSpring.service;

import BellSpring.model.Order;
import BellSpring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    /**
     * Создание заказа с использованием реактивного программирования
     */
    public Mono<Order> createOrder(String sessionId, String productName, Integer quantity) {
        return productService.getProductPrice(productName)
                .flatMap(unitPrice -> {
                    Integer totalPrice = unitPrice * quantity;

                    Order order = new Order();
                    order.setSessionId(sessionId);
                    order.setProductName(productName);
                    order.setQuantity(quantity);
                    order.setUnitPrice(unitPrice);
                    order.setTotalPrice(totalPrice);

                    // Изолируем блокирующую операцию - ЭТОГО ДОСТАТОЧНО!
                    return Mono.fromCallable(() -> orderRepository.save(order))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    /**
     * Получение заказа по ID с валидацией названия продукта
     */
    public Mono<Order> getOrderById(Long orderId, String expectedProductName) {
        return Mono.fromCallable(() -> {
                    var orderOpt = orderRepository.findById(orderId);

                    if (orderOpt.isEmpty()) {
                        throw new IllegalArgumentException("Order not found with id: " + orderId);
                    }

                    Order order = orderOpt.get();

                    // Валидация названия продукта
                    if (!order.getProductName().equals(expectedProductName)) {
                        throw new IllegalArgumentException(
                                "Product name validation failed. Expected: " + expectedProductName +
                                        ", but got: " + order.getProductName()
                        );
                    }
                    return order;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}