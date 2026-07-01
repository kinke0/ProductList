package com.superpower.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineTracker {

    private static final long ONLINE_THRESHOLD_MINUTES = 30;
    private final Map<Long, LocalDateTime> activeUsers = new ConcurrentHashMap<>();

    public void online(Long userId) {
        activeUsers.put(userId, LocalDateTime.now());
    }

    public void offline(Long userId) {
        activeUsers.remove(userId);
    }

    public void touch(Long userId) {
        if (userId != null && activeUsers.containsKey(userId)) {
            activeUsers.put(userId, LocalDateTime.now());
        }
    }

    public boolean isOnline(Long userId) {
        LocalDateTime lastActive = activeUsers.get(userId);
        if (lastActive == null) return false;
        return lastActive.plusMinutes(ONLINE_THRESHOLD_MINUTES).isAfter(LocalDateTime.now());
    }

    public Set<Long> getOnlineUserIds() {
        activeUsers.entrySet().removeIf(e ->
                e.getValue().plusMinutes(ONLINE_THRESHOLD_MINUTES).isBefore(LocalDateTime.now()));
        return activeUsers.keySet();
    }
}
