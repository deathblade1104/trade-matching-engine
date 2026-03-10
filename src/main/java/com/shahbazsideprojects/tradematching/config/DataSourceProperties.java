package com.shahbazsideprojects.tradematching.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Database (DataSource) configuration. Binds to spring.datasource.* in application properties.
 * Reference this config wherever DB settings are needed (e.g. health, logging, custom config).
 * Spring Boot still auto-configures the DataSource from the same properties.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {

    /**
     * JDBC URL (e.g. jdbc:postgresql://localhost:5432/trade_matching_engine).
     */
    private String url;

    /**
     * Database username.
     */
    private String username;

    /**
     * Database password.
     */
    private String password;

    /**
     * Driver class name (e.g. org.postgresql.Driver).
     */
    private String driverClassName;
}
