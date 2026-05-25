package com.pearl.payroll.indicative;

import com.pearl.payroll.common.dto.IndicativeEmployee;
import com.pearl.common.logging.CorrelationIdFilter;
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
public class IndicativeProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndicativeProcessingServiceApplication.class, args);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}

@RestController
@RequestMapping("/v1/indicative")
class IndicativeProcessingController {

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> acceptEmployee(@RequestBody IndicativeEmployee employee) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId", employee.employeeId());
        response.put("status", "received");
        response.put("payloadType", "indicative");
        return response;
    }

    @GetMapping("/status")
    Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "indicative-processing-service");
        response.put("capability", "indicative-demographic-processing");
        response.put("status", "ready");
        return response;
    }
}
