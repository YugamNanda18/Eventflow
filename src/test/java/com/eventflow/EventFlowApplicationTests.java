package com.eventflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventFlowApplicationTests {

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        // Verifies Spring Boot ApplicationContext loads cleanly with test profile
    }
}
