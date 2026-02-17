package BellSpring.service;

import BellSpring.metrics.UserMetrics;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final UserMetrics userMetrics;
    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 минут

    public SessionService(UserMetrics userMetrics) {
        this.userMetrics = userMetrics;
    }

    public void createSession(String sessionId) {
        activeSessions.put(sessionId, System.currentTimeMillis());
        userMetrics.userConnected();
    }

    public boolean isValidSession(String sessionId) {
        if (sessionId == null || !activeSessions.containsKey(sessionId)) {
            return false;}
        Long creationTime = activeSessions.get(sessionId);
        if (creationTime == null) {
            return false;
        }

        // Проверяем не истекла ли сессия
        if (System.currentTimeMillis() - creationTime > SESSION_TIMEOUT_MS) {
            activeSessions.remove(sessionId);
            userMetrics.userDisconnected();
            return false;
        }

        return true;
    }

    public boolean deleteSession(String sessionId) {
        boolean removed = activeSessions.remove(sessionId) != null;
        if (removed) {
            userMetrics.userDisconnected();
        }
        return removed;
    }

    public void invalidateSession(String sessionId) {
        boolean removed = activeSessions.remove(sessionId) != null;
        if (removed) {
            userMetrics.userDisconnected();
        }
    }
}
