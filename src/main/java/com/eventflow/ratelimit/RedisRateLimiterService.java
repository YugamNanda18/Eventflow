package com.eventflow.ratelimit;

import com.eventflow.common.context.TenantContext;
import com.eventflow.common.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int eventsPerMinute;

    public RedisRateLimiterService(StringRedisTemplate redisTemplate,
                                  @Value("${eventflow.rate-limiting.enabled:true}") boolean enabled,
                                  @Value("${eventflow.rate-limiting.events-per-minute:100}") int eventsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.eventsPerMinute = eventsPerMinute;
    }

    public void checkRateLimit(String keyIdentifier) {
        if (!enabled) return;

        String tenantId = TenantContext.getTenantId();
        String identifier = keyIdentifier != null ? keyIdentifier : (tenantId != null ? tenantId : "anonymous");
        long currentMinute = Instant.now().getEpochSecond() / 60;
        String redisKey = "rate_limit:" + identifier + ":" + currentMinute;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, 70, TimeUnit.SECONDS);
            }

            if (count != null && count > eventsPerMinute) {
                log.warn("Rate limit exceeded for identifier {}: {} req/min (limit {})", identifier, count, eventsPerMinute);
                throw new RateLimitExceededException("Rate limit of " + eventsPerMinute + " requests/minute exceeded for " + identifier);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis rate limit check failed, allowing request in fallback mode", e);
        }
    }
}
