package com.minimarket.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${minimarket.login.max-attempts}") int maxAttempts,
            @Value("${minimarket.login.lock-minutes}") long lockMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public boolean isBlocked(String username, String ip) {
        AttemptState state = attempts.get(key(username, ip));
        if (state == null || state.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(state.lockedUntil)) {
            attempts.remove(key(username, ip));
            return false;
        }
        return true;
    }

    public void recordSuccess(String username, String ip) {
        attempts.remove(key(username, ip));
    }

    public void recordFailure(String username, String ip) {
        String key = key(username, ip);
        attempts.compute(key, (ignored, state) -> {
            AttemptState current = state == null ? new AttemptState() : state;
            current.failures++;
            if (current.failures >= maxAttempts) {
                current.lockedUntil = Instant.now().plus(lockDuration);
            }
            return current;
        });
    }

    private String key(String username, String ip) {
        return username.toLowerCase() + "@" + ip;
    }

    private static class AttemptState {
        private int failures;
        private Instant lockedUntil;
    }
}
