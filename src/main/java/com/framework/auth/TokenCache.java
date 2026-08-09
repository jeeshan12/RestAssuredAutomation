package com.framework.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


public class TokenCache {

    private static final TokenCache INSTANCE = new TokenCache();

    private final Map<String, OAuth2Token> tokens = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private TokenCache() {
    }

    public static TokenCache getInstance() {
        return INSTANCE;
    }

    public OAuth2Token getOrFetch(String cacheKey, Supplier<OAuth2Token> fetcher) {
        OAuth2Token cached = tokens.get(cacheKey);
        if (cached != null && !cached.isExpired(30)) {
            return cached;
        }

        ReentrantLock lock = locks.computeIfAbsent(cacheKey, key -> new ReentrantLock());
        lock.lock();
        try {
            OAuth2Token recheck = tokens.get(cacheKey);
            if (recheck != null && !recheck.isExpired(30)) {
                return recheck;
            }
            OAuth2Token fresh = fetcher.get();
            tokens.put(cacheKey, fresh);
            return fresh;
        } finally {
            lock.unlock();
        }
    }

    /** Mainly for tests that need to force a re-fetch. */
    public void evict(String cacheKey) {
        tokens.remove(cacheKey);
    }
}
