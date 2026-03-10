package com.sideprojects.tradematching.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    public static final String TOPIC_PROCESS_ORDER = "process-order";
    public static final String TOPIC_EXPIRE_ORDER = "expire-order";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProcessOrder(Long orderId, int reprocessCount) {
        ProcessOrderPayload payload = ProcessOrderPayload.builder()
                .orderId(orderId)
                .reprocessCount(reprocessCount)
                .build();
        kafkaTemplate.send(TOPIC_PROCESS_ORDER, String.valueOf(orderId), payload);
        log.debug("Sent process-order for orderId={} reprocessCount={}", orderId, reprocessCount);
    }

    public void sendExpireOrder(Long orderId) {
        ExpireOrderPayload payload = ExpireOrderPayload.builder()
                .orderId(orderId)
                .build();
        kafkaTemplate.send(TOPIC_EXPIRE_ORDER, String.valueOf(orderId), payload);
        log.debug("Sent expire-order for orderId={}", orderId);
    }
}
