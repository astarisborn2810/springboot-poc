CREATE SCHEMA IF NOT EXISTS pearl_financial;
CREATE SCHEMA IF NOT EXISTS pearl_indicative;
CREATE SCHEMA IF NOT EXISTS pearl_results;

CREATE TABLE IF NOT EXISTS pearl_results.batch_status (
    batch_id VARCHAR(80) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    payload_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pearl_financial.payroll_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL,
    employee_id VARCHAR(80) NOT NULL,
    gross_pay NUMERIC(18, 2) NOT NULL,
    net_pay NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pearl_indicative.employee_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL,
    employee_id VARCHAR(80) NOT NULL,
    participant_id VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
