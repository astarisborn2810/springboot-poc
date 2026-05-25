package com.pearl.payroll.batchcompletion;

import com.pearl.payroll.common.dto.ProcessingStatus;
import com.pearl.common.logging.CorrelationIdFilter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class BatchCompletionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchCompletionServiceApplication.class, args);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}

@RestController
@RequestMapping("/v1/batch-completion")
class BatchCompletionController {

    @PostMapping("/batches/{batchId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> completeBatch(@PathVariable String batchId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batchId);
        response.put("status", ProcessingStatus.COMPLETED);
        response.put("completedAt", Instant.now());
        return response;
    }
}
