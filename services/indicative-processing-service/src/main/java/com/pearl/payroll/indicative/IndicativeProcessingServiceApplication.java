package com.pearl.payroll.indicative;

import com.pearl.common.logging.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@OpenAPIDefinition(
        info = @Info(
                title = "PEARL Indicative Processing API",
                version = "v1",
                description = "Accepts Step Functions file pointers for indicative payloads stored in S3."),
        servers = @Server(url = "http://localhost:8082", description = "Local indicative processing service"),
        tags = @Tag(name = "Indicative Processing", description = "Indicative employee intake and service status endpoints"))
@SpringBootApplication
public class IndicativeProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndicativeProcessingServiceApplication.class, args);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
