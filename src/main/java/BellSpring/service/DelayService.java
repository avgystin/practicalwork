package BellSpring.service;

import BellSpring.model.DelayConfig;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DelayService {

    private final DelayConfig delayConfig;
    private final ObservationRegistry observationRegistry;

    @Timed(value = "delay.apply",
            description = "Time taken to apply dynamic delay")
    public void applyDelay(String endpoint, long elapsedMs) {
        long targetDelay = getDelay(endpoint);

        if (targetDelay > 0) {
            long actualDelay = Math.max(0, targetDelay - elapsedMs);

            if (actualDelay == 0) {
                return;
            }

            // Создаем спан с тегами endpoint и delay.ms
            Observation observation = Observation.createNotStarted("delay." + endpoint, observationRegistry)
                    .lowCardinalityKeyValue("endpoint", endpoint)
                    .lowCardinalityKeyValue("delay.ms", String.valueOf(actualDelay));  // ДОБАВЛЕНО

            observation.start();

            try {
                // События с важной информацией
                observation.event(Observation.Event.of("target.ms: " + targetDelay));
                observation.event(Observation.Event.of("elapsed.ms: " + elapsedMs));
                observation.event(Observation.Event.of("delay.started"));

                TimeUnit.MILLISECONDS.sleep(actualDelay);

                observation.event(Observation.Event.of("delay.finished"));
                observation.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                observation.event(Observation.Event.of("delay.error"));
                observation.error(e);
                observation.stop();
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