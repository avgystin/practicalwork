package BellSpring.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class ProductService {

    // Статическое хранилище продуктов (в реальном приложении было бы в БД)
    private static final Map<String, Integer> products = new LinkedHashMap<>();

    public ProductService() {
        initializeProducts();
    }

    // Инициализация списка продуктов при запуске приложения
    private void initializeProducts() {
        products.put("MacBook", 1);
        products.put("iPhone", 89);
        products.put("Samsung Galaxy", 45);
        products.put("Sony WH", 24);
        products.put("Apple Watch", 41);
    }

    /**
     * Реактивный метод получения всех продуктов
     * Возвращает Mono<Map> для асинхронной работы в реактивных цепочках
     */
    public Mono<Map<String, Integer>> getAllProducts() {
        // Mono.just создает реактивный поток с одним элементом
        return Mono.just(Collections.unmodifiableMap(products));
    }

    /**
     * Реактивный метод получения цены продукта по имени
     * Использует Mono.fromSupplier для ленивого выполнения
     */
    public Mono<Integer> getProductPrice(String productName) {
        return Mono.fromSupplier(() -> {
            Integer price = products.get(productName);
            if (price == null) {
                throw new IllegalArgumentException("Product not found: " + productName);
            }
            return price;
        });
    }
}