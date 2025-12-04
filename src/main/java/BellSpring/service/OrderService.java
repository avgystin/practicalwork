package BellSpring.service;

import BellSpring.model.Order;
import BellSpring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

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
                    order.setOrderUuid(UUID.randomUUID().toString());
                    order.setCreatedAt(LocalDateTime.now());

                    // R2DBC: save возвращает Mono<Order>
                    return orderRepository.save(order);
                });
    }

    /**
     * Получение заказа по ID с валидацией названия продукта
     */
    public Mono<Order> getOrderById(Long orderId, String expectedProductName) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found with id: " + orderId)))
                .flatMap(order -> {
                    if (!order.getProductName().equals(expectedProductName)) {
                        return Mono.error(new IllegalArgumentException(
                                "Product name validation failed. Expected: " + expectedProductName +
                                        ", but got: " + order.getProductName()
                        ));
                    }
                    return Mono.just(order);
                });
    }
}