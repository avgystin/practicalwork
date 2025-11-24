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
        Long delay = delayConfig.getDelays().get(endpoint);
        if (delay != null && delay > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public DelayConfig getConfig() {
        return delayConfig;
    }

    public void updateDelays(java.util.Map<String, Long> newDelays) {
        delayConfig.updateDelays(newDelays);
    }
}