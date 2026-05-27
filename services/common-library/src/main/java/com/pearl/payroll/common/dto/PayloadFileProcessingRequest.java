package com.pearl.payroll.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PayloadFileProcessingRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String s3PathOrArn) {

    @AssertTrue(message = "fileName must use batchId_vendorName_plan format.")
    @JsonIgnore
    public boolean isFileNameFormatValid() {
        return PayloadFileNameMetadata.isValid(fileName);
    }

    @AssertTrue(message = "s3PathOrArn must be an s3:// URI or arn:aws:s3 object ARN.")
    @JsonIgnore
    public boolean isS3LocationFormatValid() {
        if (s3PathOrArn == null || s3PathOrArn.isBlank()) {
            return true;
        }
        String value = s3PathOrArn.trim();
        return value.matches("^s3://[^/]+/.+")
                || value.matches("^arn:aws[a-zA-Z-]*:s3:::.+/.+");
    }

    public PayloadFileNameMetadata fileNameMetadata() {
        return PayloadFileNameMetadata.from(fileName);
    }
}
