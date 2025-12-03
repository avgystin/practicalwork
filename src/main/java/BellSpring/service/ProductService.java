package BellSpring.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class ProductService {

    private static final Map<String, Integer> products = new LinkedHashMap<>();

    public ProductService() {
        initializeProducts();
    }

    private void initializeProducts() {
        products.put("MacBook", 1);
        products.put("iPhone", 89);
        products.put("Samsung Galaxy", 45);
        products.put("Sony WH", 24);
        products.put("Apple Watch", 41);
    }

    // Реактивные методы
    public Mono<Map<String, Integer>> getAllProducts() {
        return Mono.just(Collections.unmodifiableMap(products));
    }

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