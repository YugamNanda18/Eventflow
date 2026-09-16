package com.eventflow.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_EVENTS = "eventflow.events";
    public static final String TOPIC_WEBHOOKS = "eventflow.webhooks";
    public static final String TOPIC_NOTIFICATIONS = "eventflow.notifications";
    public static final String TOPIC_AUDIT = "eventflow.audit";
    public static final String TOPIC_RETRY = "eventflow.retry";
    public static final String TOPIC_DLQ = "eventflow.dlq";

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(TOPIC_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic webhooksTopic() {
        return TopicBuilder.name(TOPIC_WEBHOOKS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(TOPIC_AUDIT).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic retryTopic() {
        return TopicBuilder.name(TOPIC_RETRY).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(TOPIC_DLQ).partitions(3).replicas(1).build();
    }
}
