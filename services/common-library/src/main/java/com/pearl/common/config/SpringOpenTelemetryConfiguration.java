package com.pearl.common.config;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpringOpenTelemetryConfiguration {

    @Bean
    public OpenTelemetry pearlOpenTelemetry() {
        return OpenTelemetryConfig.create(OpenTelemetryProperties.fromEnvironment()).openTelemetry();
    }
}
