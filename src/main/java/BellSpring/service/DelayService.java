package BellSpring.service;

import BellSpring.model.DelayConfig;
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

    public void applyDelay(String endpoint) {
        long delay = getDelay(endpoint);
        if (delay > 0) {
            // Создаем вложенный спан для операции задержки
            Observation observation = Observation.createNotStarted("delay." + endpoint, observationRegistry)
                    .lowCardinalityKeyValue("endpoint", endpoint)
                    .lowCardinalityKeyValue("delay.ms", String.valueOf(delay));

            // Начинаем наблюдение
            observation.start();

            try {
                // Аннотация начала задержки
                observation.event(Observation.Event.of("delay.started"));

                TimeUnit.MILLISECONDS.sleep(delay);

                // Аннотация конца задержки
                observation.event(Observation.Event.of("delay.finished"));

                // Завершаем успешно
                observation.stop();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Аннотация ошибки
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