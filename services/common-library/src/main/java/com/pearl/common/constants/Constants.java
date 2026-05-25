package com.pearl.common.constants;

public final class Constants {

    public static final String PLATFORM_NAME = "pearl-payroll-platform";
    public static final String DEFAULT_SERVICE_NAME = "pearl-service";
    public static final String DEFAULT_TENANT_ID = "pearl";

    private Constants() {
    }

    public static final class Headers {
        public static final String CORRELATION_ID = "X-Correlation-Id";
        public static final String BATCH_ID = "X-Batch-Id";
        public static final String VENDOR_ID = "X-Vendor-Id";
        public static final String PLAN_ID = "X-Plan-Id";
        public static final String EXECUTION_ID = "X-Execution-Id";
        public static final String REQUEST_ID = "X-Request-Id";
        public static final String TENANT_ID = "X-Tenant-Id";

        private Headers() {
        }
    }

    public static final class MdcKeys {
        public static final String CORRELATION_ID = "correlationId";
        public static final String BATCH_ID = "batchId";
        public static final String VENDOR_ID = "vendorId";
        public static final String PLAN_ID = "planId";
        public static final String EXECUTION_ID = "executionId";
        public static final String REQUEST_ID = "requestId";
        public static final String TENANT_ID = "tenantId";
        public static final String TRACE_ID = "traceId";
        public static final String SPAN_ID = "spanId";

        private MdcKeys() {
        }
    }

    public static final class TracingKeys {
        public static final String CORRELATION_ID = "pearl.correlation.id";
        public static final String BATCH_ID = "pearl.batch.id";
        public static final String VENDOR_ID = "pearl.vendor.id";
        public static final String PLAN_ID = "pearl.plan.id";
        public static final String EXECUTION_ID = "pearl.execution.id";
        public static final String REQUEST_ID = "pearl.request.id";
        public static final String EVENT_TYPE = "pearl.event.type";
        public static final String PAYLOAD_TYPE = "pearl.payload.type";
        public static final String RETRYABLE = "pearl.error.retryable";

        private TracingKeys() {
        }
    }

    public static final class AwsPrefixes {
        public static final String S3_PAYLOADS = "pearl-payroll-payloads";
        public static final String S3_AUDIT = "pearl-payroll-audit";
        public static final String SQS_BATCH_EVENTS = "pearl-batch-events";
        public static final String SQS_COMPLETION_EVENTS = "pearl-completion-events";
        public static final String DYNAMODB_BATCH_STATUS = "pearl-batch-status";
        public static final String DYNAMODB_PARTICIPANT_STATUS = "pearl-participant-status";
        public static final String STEP_FUNCTION_DOWNSTREAM = "pearl-payroll-downstream";

        private AwsPrefixes() {
        }
    }

    public static final class StatusValues {
        public static final String RECEIVED = "RECEIVED";
        public static final String VALIDATING = "VALIDATING";
        public static final String ROUTED = "ROUTED";
        public static final String PROCESSING = "PROCESSING";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
        public static final String REJECTED = "REJECTED";

        private StatusValues() {
        }
    }

    public static final class EventTypes {
        public static final String FINANCIAL_PAYLOAD_RECEIVED = "financial.payload.received";
        public static final String INDICATIVE_PAYLOAD_RECEIVED = "indicative.payload.received";
        public static final String BATCH_STARTED = "batch.started";
        public static final String BATCH_COMPLETED = "batch.completed";
        public static final String BATCH_FAILED = "batch.failed";
        public static final String MANUAL_REVIEW_REQUIRED = "manual.review.required";

        private EventTypes() {
        }
    }
}
