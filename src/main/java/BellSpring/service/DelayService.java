package BellSpring.service;

import BellSpring.model.DelayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DelayService {

    private final DelayConfig delayConfig;

    public void applyDelay(String endpoint) {
        long delay = getDelay(endpoint);
        if (delay > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private long getDelay(String endpoint) {
        return switch (endpoint) {
            case "session.create" -> delayConfig.getSession().getCreate();
            case "session.delete" -> delayConfig.getSession().getDelete();
            case "session.check" -> delayConfig.getSession().getCheck();
            case "order.create" -> delayConfig.getOrder().getCreate();
            case "order.getProducts" -> delayConfig.getOrder().getGetProducts();
            case "order.getOrder" -> delayConfig.getOrder().getGetOrder();
            default -> delayConfig.getDefaultDelay();
        };
    }

    public DelayConfig getConfig() {
        return delayConfig;
    }
}