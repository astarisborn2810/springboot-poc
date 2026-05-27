package com.pearl.payroll.common.dto;

import java.time.Instant;

public record PayloadFileAcceptedResponse(
        String fileName,
        String batchId,
        String vendorName,
        String plan,
        String s3PathOrArn,
        String payloadType,
        ProcessingStatus status,
        Instant receivedAt) {

    public static PayloadFileAcceptedResponse from(
            PayloadFileProcessingRequest request,
            String payloadType,
            Instant receivedAt) {
        PayloadFileNameMetadata metadata = request.fileNameMetadata();
        return new PayloadFileAcceptedResponse(
                request.fileName(),
                metadata.batchId(),
                metadata.vendorName(),
                metadata.plan(),
                request.s3PathOrArn(),
                payloadType,
                ProcessingStatus.RECEIVED,
                receivedAt == null ? Instant.now() : receivedAt);
    }
}
