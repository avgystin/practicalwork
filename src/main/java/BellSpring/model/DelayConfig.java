package BellSpring.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "response.delay")
public class DelayConfig {
    private long defaultDelay;
    private Session session = new Session();
    private Order order = new Order();

    @Data
    public static class Session {
        private long create;
        private long delete;
        private long check;
    }

    @Data
    public static class Order {
        private long create;
        private long getProducts;
        private long getOrder;
    }
}