package BellSpring.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    public static Map<String, Integer> getAllProducts() {
        return Collections.unmodifiableMap(products);
    }
    public Integer getProductPrice(String productName) {
        return products.get(productName);
    }
}