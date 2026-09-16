package com.eventflow.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Configuration
public class StandaloneFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<String, String> fallbackKafkaTemplate() {
        return new KafkaTemplate<String, String>(dummyProducerFactory()) {
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    public ProducerFactory<String, String> dummyProducerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "localhost:9092");
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate fallbackStringRedisTemplate() {
        return new StringRedisTemplate() {
            @Override
            public void afterPropertiesSet() {
                // No-op to prevent "RedisConnectionFactory is required" assertion during container initialization
            }
        };
    }
}
