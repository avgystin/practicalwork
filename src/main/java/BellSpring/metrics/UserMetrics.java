package BellSpring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class UserMetrics {

    private final AtomicLong concurrentUsers = new AtomicLong(0);

    public UserMetrics(MeterRegistry meterRegistry) {
        meterRegistry.gauge("users.concurrent", concurrentUsers);
    }

    public void userConnected() {
        concurrentUsers.incrementAndGet();
    }

    public void userDisconnected() {
        concurrentUsers.decrementAndGet();
    }
}