package BellSpring.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class ProductService {
    private static final Map<String, Integer> PRODUCTS = Map.of(
            "MacBook", 1,
            "iPhone", 89,
            "Samsung Galaxy", 45,
            "Sony WH", 24,
            "Apple Watch", 41
    );

    /**
     * Реактивный метод получения всех продуктов
     * Возвращает Mono<Map> для асинхронной работы в реактивных цепочках
     */
    public Mono<Map<String, Integer>> getAllProducts() {
        // Mono.just создает реактивный поток с одним элементом
        return Mono.just(PRODUCTS);
    }

    /**
     * Реактивный метод получения цены продукта по имени
     * Использует Mono.fromSupplier для ленивого выполнения
     */
    public Mono<Integer> getProductPrice(String productName) {
        return Mono.fromSupplier(() -> {
            Integer price = PRODUCTS.get(productName);
            if (price == null) {
                throw new IllegalArgumentException("Product not found: " + productName);
            }
            return price;
        });
    }
}