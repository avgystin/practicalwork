package BellSpring.service;

import BellSpring.metrics.UserMetrics;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final UserMetrics userMetrics;
    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();
    private final long sessionTimeoutMs;

    public SessionService(
            UserMetrics userMetrics,
            @Value("${session.timeout.minutes:30}") int sessionTimeoutMinutes) { // ДОБАВИТЬ: параметр с значением по умолчанию
        this.userMetrics = userMetrics;
        // ДОБАВИТЬ: конвертация минут в миллисекунды
        this.sessionTimeoutMs = sessionTimeoutMinutes * 60 * 1000L;
    }

    @Timed(value = "session.create",
            description = "Time taken to create session")
    public void createSession(String sessionId) {
        activeSessions.put(sessionId, System.currentTimeMillis());
        userMetrics.userConnected();
    }

    @Timed(value = "session.validate",
            description = "Time taken to validate session")
    public boolean isValidSession(String sessionId) {
        if (sessionId == null || !activeSessions.containsKey(sessionId)) {
            return false;
        }

        Long creationTime = activeSessions.get(sessionId);
        if (creationTime == null) {
            return false;
        }

        // Проверяем не истекла ли сессия
        if (System.currentTimeMillis() - creationTime > sessionTimeoutMs) {
            activeSessions.remove(sessionId);
            userMetrics.userDisconnected();
            return false;
        }
        return true;
    }

    @Timed(value = "session.delete",
            description = "Time taken to delete session")
    public boolean deleteSession(String sessionId) {
        boolean removed = activeSessions.remove(sessionId) != null;
        if (removed) {
            userMetrics.userDisconnected();
        }
        return removed;
    }

    /**
     * Периодическая очистка истекших сессий.
     * Запускается каждую минуту.
     */
    @Scheduled(fixedDelay = 60000)
    @Timed(value = "session.cleanup",
            description = "Time taken to cleanup expired sessions")
    @SuppressWarnings("unused") // Метод вызывается Spring-ом автоматически
    public void cleanupExpiredSessions() {
        int beforeSize = activeSessions.size();
        long now = System.currentTimeMillis();

        activeSessions.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue() > sessionTimeoutMs;
            if (expired) {
                userMetrics.userDisconnected();
            }
            return expired;
        });
    }
}