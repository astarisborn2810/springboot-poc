package com.pearl.payroll.financial;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FinancialProcessingController.class)
@Import(FinancialApiExceptionHandler.class)
class FinancialProcessingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void acceptFileReturnsParsedFileMetadata() throws Exception {
        Map<String, Object> request = Map.of(
                "fileName", "batch-20260522_prismhr_PEARL-401K-PLAN-001",
                "s3PathOrArn", "s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json");

        mockMvc.perform(post("/v1/financial/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.payloadType").value("financial"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.fileName").value("batch-20260522_prismhr_PEARL-401K-PLAN-001"))
                .andExpect(jsonPath("$.batchId").value("batch-20260522"))
                .andExpect(jsonPath("$.vendorName").value("prismhr"))
                .andExpect(jsonPath("$.plan").value("PEARL-401K-PLAN-001"))
                .andExpect(jsonPath("$.s3PathOrArn").value("s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json"));
    }

    @Test
    void acceptFileRejectsMissingRequiredOpenApiFields() throws Exception {
        mockMvc.perform(post("/v1/financial/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void acceptFileRejectsFileNameThatDoesNotMatchStepFunctionFormat() throws Exception {
        Map<String, Object> request = Map.of(
                "fileName", "batch-20260522",
                "s3PathOrArn", "s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/file.json");

        mockMvc.perform(post("/v1/financial/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
