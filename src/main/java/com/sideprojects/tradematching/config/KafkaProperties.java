package com.sideprojects.tradematching.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka configuration. Bind to spring.kafka.bootstrap-servers in application properties.
 * Reference this config wherever Kafka settings are needed (e.g. KafkaConfig, KafkaConsumerConfig).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

    /**
     * Comma-separated bootstrap servers (e.g. localhost:9092).
     */
    private String bootstrapServers;
}
