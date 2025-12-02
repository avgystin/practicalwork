package BellSpring.service;

import BellSpring.model.DelayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DelayService {

    private final DelayConfig delayConfig;

    public Mono<Void> applyDelay(String endpoint) {
        Long delay = delayConfig.getDelays().get(endpoint);
        if (delay != null && delay > 0) {
            return Mono.delay(Duration.ofMillis(delay)).then();
        }
        return Mono.empty();
    }

    public DelayConfig getConfig() {
        return delayConfig;
    }

    public void updateDelays(Map<String, Long> newDelays) {
        delayConfig.updateDelays(newDelays);
    }
}