package com.pearl.payroll.financial;

import com.pearl.payroll.common.dto.PayrollBatch;
import com.pearl.payroll.common.dto.ProcessingStatus;
import com.pearl.common.logging.CorrelationIdFilter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@RequestMapping("/v1/financial")
class FinancialProcessingController {

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.ACCEPTED)
    PayrollBatch acceptBatch(@RequestBody PayrollBatch batch) {
        return new PayrollBatch(
                batch.batchId(),
                batch.tenantId(),
                batch.sourceSystem(),
                "financial",
                ProcessingStatus.RECEIVED,
                batch.receivedAt() == null ? Instant.now() : batch.receivedAt(),
                batch.attributes());
    }

    @GetMapping("/status")
    Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "financial-processing-service");
        response.put("capability", "financial-payroll-processing");
        response.put("status", "ready");
        return response;
    }
}
