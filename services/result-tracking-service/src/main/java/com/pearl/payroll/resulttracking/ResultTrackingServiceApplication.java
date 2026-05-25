package com.pearl.payroll.resulttracking;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ResultTrackingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultTrackingServiceApplication.class, args);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}

@RestController
@RequestMapping("/v1/results")
class ResultTrackingController {

    @GetMapping("/batches/{batchId}")
    PayrollBatch getBatch(@PathVariable String batchId) {
        return new PayrollBatch(
                batchId,
                "pearl",
                "mock-local",
                "unknown",
                ProcessingStatus.PROCESSING,
                Instant.now(),
                Map.of("source", "placeholder"));
    }

    @PostMapping("/batches/{batchId}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> updateStatus(@PathVariable String batchId, @RequestBody StatusUpdate update) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batchId);
        response.put("status", update.status());
        response.put("reason", update.reason());
        return response;
    }

    record StatusUpdate(ProcessingStatus status, String reason) {
    }
}
