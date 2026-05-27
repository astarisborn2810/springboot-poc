package com.pearl.payroll.indicative;

import com.pearl.common.constants.Constants;
import com.pearl.common.dto.ServiceStatusResponse;
import com.pearl.payroll.common.constants.PearlConstants;
import com.pearl.payroll.common.dto.PayloadFileAcceptedResponse;
import com.pearl.payroll.common.dto.PayloadFileProcessingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(value = "/v1/indicative", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Indicative Processing")
class IndicativeProcessingController {

    @Operation(
            summary = "Accept an indicative payload file",
            description = "Accepts the Step Functions file pointer for an indicative payload. The fileName must use batchId_vendorName_plan format. Employee, participant, and benefit records are read from the S3 file during processing.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Indicative file pointer from Step Functions.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayloadFileProcessingRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject("""
                                    {
                                      "fileName": "batch-20260522_prismhr_PEARL-401K-PLAN-001",
                                      "s3PathOrArn": "s3://payroll-outbound-dev/outbound/prismhr/indicative/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json"
                                    }
                                    """))))
    @Parameter(
            name = Constants.Headers.CORRELATION_ID,
            in = ParameterIn.HEADER,
            description = "Optional correlation id echoed in the response headers.",
            schema = @Schema(type = "string"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Indicative payload file accepted.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayloadFileAcceptedResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request body failed validation.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported media type.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/files", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    PayloadFileAcceptedResponse acceptFile(@Valid @RequestBody PayloadFileProcessingRequest request) {
        return PayloadFileAcceptedResponse.from(request, PearlConstants.INDICATIVE_PAYLOAD_TYPE, Instant.now());
    }

    @Operation(summary = "Get indicative service status")
    @ApiResponse(
            responseCode = "200",
            description = "Indicative service status.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServiceStatusResponse.class)))
    @GetMapping("/status")
    ServiceStatusResponse status() {
        return new ServiceStatusResponse(
                "indicative-processing-service",
                "indicative-demographic-processing",
                "ready");
    }
}
