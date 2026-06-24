package com.afet.monitoring.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the earthquake topic. Spring's KafkaAdmin auto-creates any {@link NewTopic}
 * bean on startup, so the topic exists before the first publish. 3 partitions allow
 * parallel consumption; key = earthquake id keeps per-quake ordering within a partition.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic earthquakeDetectedTopic() {
        return TopicBuilder.name(KafkaTopics.EARTHQUAKE_DETECTED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
