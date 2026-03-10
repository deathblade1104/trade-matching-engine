package com.shahbazsideprojects.tradematching.util;

import com.shahbazsideprojects.tradematching.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheKeyUtil {

    private final AppProperties appProperties;

    /**
     * Build Redis cache key: {serviceName}:{env}:{module}:{resource}:{identifier}[{:extraParts}]
     */
    public String getCacheKey(String module, String resource, String identifier, String... additionalParts) {
        String base = appProperties.getServiceName() + ":" + appProperties.getEnv() + ":"
                + module + ":" + resource + ":" + identifier;
        if (additionalParts != null && additionalParts.length > 0) {
            base = base + ":" + String.join(":", additionalParts);
        }
        return base;
    }
}
