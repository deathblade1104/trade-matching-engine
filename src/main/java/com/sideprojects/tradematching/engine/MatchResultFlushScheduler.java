package com.sideprojects.tradematching.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * When {@code app.matching.persistence.batching-enabled=true}, flushes the match-result
 * buffer to the DB on a fixed interval. This controls write throughput by batching
 * many match results into fewer, larger transactions.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.matching.persistence.batching-enabled", havingValue = "true")
public class MatchResultFlushScheduler {

    private final MatchResultWriteBuffer writeBuffer;

    @Scheduled(fixedDelayString = "${app.matching.persistence.flush-interval-ms:200}", initialDelayString = "${app.matching.persistence.flush-interval-ms:200}")
    public void flushBuffer() {
        writeBuffer.flush();
    }
}
