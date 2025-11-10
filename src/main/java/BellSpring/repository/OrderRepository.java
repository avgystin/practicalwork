package BellSpring.repository;

import BellSpring.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderUuid(String orderUuid);

    List<Order> findBySessionId(String sessionId);

    List<Order> findByProductName(String productName);
}
