package com.pearl.payroll.financial;

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
                title = "PEARL Financial Processing API",
                version = "v1",
                description = "Accepts Step Functions file pointers for financial payroll payloads stored in S3."),
        servers = @Server(url = "http://localhost:8081", description = "Local financial processing service"),
        tags = @Tag(name = "Financial Processing", description = "Financial payroll batch intake and service status endpoints"))
@SpringBootApplication
public class FinancialProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialProcessingServiceApplication.class, args);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
