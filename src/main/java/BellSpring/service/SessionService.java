package BellSpring.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    // Хранилище активных сессий: sessionId -> время создания
    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();

    // Таймаут сессии: 30 минут
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;

    /**
     * Создание новой сессии
     * @return Mono<String> - реактивный результат с ID сессии
     */
    public Mono<String> createSession() {
        return Mono.fromSupplier(() -> {
            // Генерируем уникальный ID сессии
            String sessionId = UUID.randomUUID().toString();
            // Сохраняем с текущим временем
            activeSessions.put(sessionId, System.currentTimeMillis());
            return sessionId;
        });
    }

    /**
     * Проверка валидности сессии
     * @param sessionId ID сессии для проверки
     * @return Mono<Boolean> - true если сессия валидна
     */
    public Mono<Boolean> isValidSession(String sessionId) {
        return Mono.fromSupplier(() -> {
            if (sessionId == null || !activeSessions.containsKey(sessionId)) {
                return false;
            }

            Long creationTime = activeSessions.get(sessionId);
            if (creationTime == null) {
                return false;
            }

            // Проверяем не истекла ли сессия (30 минут)
            if (System.currentTimeMillis() - creationTime > SESSION_TIMEOUT_MS) {
                // Удаляем просроченную сессию
                activeSessions.remove(sessionId);
                return false;
            }

            return true;
        });
    }

    /**
     * Удаление сессии
     * @param sessionId ID сессии для удаления
     * @return Mono<Boolean> - true если сессия была удалена
     */
    public Mono<Boolean> deleteSession(String sessionId) {
        return Mono.fromSupplier(() ->
                activeSessions.remove(sessionId) != null
        );
    }
}