package com.sideprojects.tradematching.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String serviceName = "trade-matching-service";
    private String env = "development";
    private Redis redis = new Redis();
    private Matching matching = new Matching();

    @Getter
    @Setter
    public static class Redis {
        private long ttl = 3600;
    }

    /**
     * Matching engine and persistence tuning.
     * Use persistence.flush-interval-ms to control DB write throughput (batch when > 0).
     */
    @Getter
    @Setter
    public static class Matching {
        private Persistence persistence = new Persistence();
    }

    @Getter
    @Setter
    public static class Persistence {
        /** When true, buffer match results and flush on a schedule to control DB throughput. */
        private boolean batchingEnabled = false;
        /** Flush interval in ms when batching is enabled. */
        private long flushIntervalMs = 200;
        /** Max match results to merge and persist in one transaction per flush. */
        private int batchSize = 100;
    }
}
