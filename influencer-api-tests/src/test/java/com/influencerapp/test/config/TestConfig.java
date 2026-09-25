package com.influencerapp.test.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads dynamic configuration properties with fallback to system properties and environment variables.
 * Allows seamless switching between remote deployment (192.168.100.10:8080) and CI ephemeral deployment (localhost:8080).
 */
@Slf4j
@Getter
public class TestConfig {

    private static final String DEFAULT_PROPERTIES = "application-test.properties";
    private static TestConfig instance;

    private final String baseUrl;
    private final int connectionTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int asyncPollingSeconds;
    private final int pollingIntervalSeconds;

    private TestConfig() {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (Exception e) {
            log.warn("Could not load {}, relying on defaults and system properties", DEFAULT_PROPERTIES, e);
        }

        // Priority: 1. System property (-Dtarget.base.url), 2. Environment variable (TARGET_BASE_URL), 3. application-test.properties, 4. Fallback
        String resolvedBaseUrl = System.getProperty("target.base.url");
        if (resolvedBaseUrl == null || resolvedBaseUrl.isBlank()) {
            resolvedBaseUrl = System.getenv("TARGET_BASE_URL");
        }
        if (resolvedBaseUrl == null || resolvedBaseUrl.isBlank()) {
            resolvedBaseUrl = properties.getProperty("target.base.url", "http://192.168.100.10:8080");
        }
        this.baseUrl = resolvedBaseUrl.replaceAll("/+$", ""); // strip trailing slashes

        this.connectionTimeoutSeconds = Integer.parseInt(
                System.getProperty("timeout.connection.seconds",
                        properties.getProperty("timeout.connection.seconds", "10")));
        this.readTimeoutSeconds = Integer.parseInt(
                System.getProperty("timeout.read.seconds",
                        properties.getProperty("timeout.read.seconds", "30")));
        this.asyncPollingSeconds = Integer.parseInt(
                System.getProperty("timeout.async.polling.seconds",
                        properties.getProperty("timeout.async.polling.seconds", "90")));
        this.pollingIntervalSeconds = Integer.parseInt(
                System.getProperty("polling.interval.seconds",
                        properties.getProperty("polling.interval.seconds", "3")));

        log.info("Initialized TestConfig with target Base URL: {}", this.baseUrl);
    }

    public static synchronized TestConfig getInstance() {
        if (instance == null) {
            instance = new TestConfig();
        }
        return instance;
    }
}
