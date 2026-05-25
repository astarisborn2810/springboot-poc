package com.pearl.payroll.config;

import com.pearl.common.logging.CorrelationIdFilter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}

@RestController
@RequestMapping("/v1/config")
class RuntimeConfigController {

    private final Environment environment;

    RuntimeConfigController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/runtime")
    Map<String, Object> runtime() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("environment", environment.getProperty("PEARL_ENV", "local"));
        config.put("awsRegion", environment.getProperty("AWS_REGION", "us-east-1"));
        config.put("featureFlags", Map.of("stepFunctionsRouting", true, "auditTracking", true));
        return config;
    }

    @GetMapping("/services")
    List<String> services() {
        return List.of(
                "financial-processing-service",
                "indicative-processing-service",
                "result-tracking-service",
                "batch-completion-service",
                "config-service");
    }
}
