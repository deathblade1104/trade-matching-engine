package com.sideprojects.tradematching.service;

import com.sideprojects.tradematching.util.CacheKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyUtil cacheKeyUtil;

    public void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis should not be called with an empty or undefined/null key");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        validateKey(key);
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void setValue(String key, Object value, long ttlMs) {
        validateKey(key);
        if (ttlMs > 0) {
            redisTemplate.opsForValue().set(key, value, ttlMs, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    public void delete(String key) {
        validateKey(key);
        redisTemplate.delete(key);
    }

    public String getCacheKey(String module, String resource, String identifier, String... additionalParts) {
        return cacheKeyUtil.getCacheKey(module, resource, identifier, additionalParts);
    }
}
