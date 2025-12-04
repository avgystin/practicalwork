package BellSpring.repository;

import BellSpring.model.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Mono<Order> findByOrderUuid(String orderUuid);
    Flux<Order> findBySessionId(String sessionId);
    Flux<Order> findByProductName(String productName);
}