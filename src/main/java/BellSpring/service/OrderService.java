package BellSpring.service;

import BellSpring.model.Order;
import BellSpring.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    /**
     * Создает заказ для указанного товара
     */
    @Transactional
    public Mono<Order> createOrder(String sessionId, String productName, Integer quantity) {
        return productService.getProductPrice(productName)
                .flatMap(unitPrice -> {
                    // Рассчитываем общую стоимость
                    Integer totalPrice = unitPrice * quantity;

                    // Создаем и сохраняем заказ
                    Order order = new Order();
                    order.setSessionId(sessionId);
                    order.setProductName(productName);
                    order.setQuantity(quantity);
                    order.setUnitPrice(unitPrice);
                    order.setTotalPrice(totalPrice);

                    return Mono.just(orderRepository.save(order));
                });
    }

    /**
     * Получает заказ по ID с валидацией названия продукта
     */
    public Mono<Order> getOrderById(Long orderId, String expectedProductName) {
        return Mono.fromSupplier(() -> {
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isEmpty()) {
                throw new IllegalArgumentException("Order not found with id: " + orderId);
            }

            Order order = orderOpt.get();

            // Валидация по наименованию товара
            if (!order.getProductName().equals(expectedProductName)) {
                throw new IllegalArgumentException(
                        "Product name validation failed. Expected: " + expectedProductName +
                                ", but got: " + order.getProductName()
                );
            }
            return order;
        });
    }
}