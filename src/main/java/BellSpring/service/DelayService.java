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

    /**
     * Применяет задержку для указанного эндпоинта
     * @param endpoint название эндпоинта (например, "order.create")
     * @return Mono<Void> - реактивная задержка
     */
    public Mono<Void> applyDelay(String endpoint) {
        // Получаем значение задержки из конфигурации
        Long delay = delayConfig.getDelays().get(endpoint);
        if (delay != null && delay > 0) {
            // Создаем задержку с помощью Mono.delay
            return Mono.delay(Duration.ofMillis(delay)).then();
        }
        return Mono.empty(); // Если задержка 0 или не задана
    }

    /**
     * Получение текущей конфигурации задержек
     */
    public DelayConfig getConfig() {
        return delayConfig;
    }

    /**
     * Обновление задержек
     * @param newDelays новые значения задержек
     */
    public void updateDelays(Map<String, Long> newDelays) {
        delayConfig.updateDelays(newDelays);
    }
}