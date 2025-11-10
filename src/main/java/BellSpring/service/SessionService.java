package BellSpring.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 минут

    public void createSession(String sessionId) {
        activeSessions.put(sessionId, System.currentTimeMillis());
    }

    public boolean isValidSession(String sessionId) {
        if (sessionId == null || !activeSessions.containsKey(sessionId)) {
            return false;
        }

        Long creationTime = activeSessions.get(sessionId);
        if (creationTime == null) {
            return false;
        }

        // Проверяем не истекла ли сессия
        if (System.currentTimeMillis() - creationTime > SESSION_TIMEOUT_MS) {
            activeSessions.remove(sessionId);
            return false;
        }

        return true;
    }
    /**
     * Удаляет сессию по UUID
     */
    public boolean deleteSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        return activeSessions.remove(sessionId) != null;
    }

    public void invalidateSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
}
