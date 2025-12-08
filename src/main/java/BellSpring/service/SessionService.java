package BellSpring.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    // Хранилище активных сессий: sessionId -> время создания
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    // Таймаут сессии: 30 минут
    private static final long TIMEOUT = 30 * 60 * 1000;
    /**
     * Создание новой сессии
     */
    public Mono<String> createSession() {
        return Mono.fromSupplier(() -> {
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, System.currentTimeMillis());
            return sessionId;
        });
    }
    /**
     * Проверка валидности сессии
     */
    public Mono<Boolean> isValidSession(String sessionId) {
        return Mono.fromSupplier(() -> {
            Long time = sessions.get(sessionId);
            if (time == null) return false;

            // Проверяем не истекла ли сессия
            if (System.currentTimeMillis() - time > TIMEOUT) {
                sessions.remove(sessionId);
                return false;
            }
            return true;
        });
    }
    /**
     * Удаление сессии
     */
    public Mono<Boolean> deleteSession(String sessionId) {
        return Mono.fromSupplier(() -> sessions.remove(sessionId) != null
        );
    }
}