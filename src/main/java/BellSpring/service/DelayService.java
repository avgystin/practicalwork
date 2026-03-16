package BellSpring.service;

import BellSpring.model.DelayConfig;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DelayService {

    private final DelayConfig delayConfig;
    private final ObservationRegistry observationRegistry;
    private DelayConfig originalConfig;  // копия для сброса

    @PostConstruct
    public void init() {
        // Сохраняем копию исходных значений
        this.originalConfig = copyConfig(delayConfig);
    }

    private DelayConfig copyConfig(DelayConfig source) {
        DelayConfig copy = new DelayConfig();
        copy.setDefaultDelay(source.getDefaultDelay());

        // Копируем session
        DelayConfig.Session sessionCopy = new DelayConfig.Session();
        sessionCopy.setCreate(source.getSession().getCreate());
        sessionCopy.setDelete(source.getSession().getDelete());
        sessionCopy.setCheck(source.getSession().getCheck());
        copy.setSession(sessionCopy);

        // Копируем order
        DelayConfig.Order orderCopy = new DelayConfig.Order();
        orderCopy.setCreate(source.getOrder().getCreate());
        orderCopy.setGetProducts(source.getOrder().getGetProducts());
        orderCopy.setGetOrder(source.getOrder().getGetOrder());
        copy.setOrder(orderCopy);

        return copy;
    }

    @Timed(value = "delay.apply",
            description = "Time taken to apply dynamic delay")
    public void applyDelay(String endpoint, long elapsedMs) {
        long targetDelay = getDelay(endpoint);

        if (targetDelay > 0) {
            long actualDelay = Math.max(0, targetDelay - elapsedMs);

            if (actualDelay == 0) {
                return;
            }

            Observation observation = Observation.createNotStarted("delay." + endpoint, observationRegistry)
                    .lowCardinalityKeyValue("endpoint", endpoint)
                    .lowCardinalityKeyValue("delay.ms", String.valueOf(actualDelay));

            observation.start();

            try {
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

    public void resetToOriginal() {
        // Восстанавливаем из сохранённой копии
        this.delayConfig.setDefaultDelay(originalConfig.getDefaultDelay());
        this.delayConfig.getSession().setCreate(originalConfig.getSession().getCreate());
        this.delayConfig.getSession().setDelete(originalConfig.getSession().getDelete());
        this.delayConfig.getSession().setCheck(originalConfig.getSession().getCheck());
        this.delayConfig.getOrder().setCreate(originalConfig.getOrder().getCreate());
        this.delayConfig.getOrder().setGetProducts(originalConfig.getOrder().getGetProducts());
        this.delayConfig.getOrder().setGetOrder(originalConfig.getOrder().getGetOrder());

        System.out.println("Config reset to original: " + originalConfig);
    }

    public DelayConfig getConfig() {
        return delayConfig;
    }
}