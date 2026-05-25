# Observability

Local observability starts with the collector in `local/docker-compose/otel-collector-config.yml`.

Application defaults:

- JSON logs through `logstash-logback-encoder`
- Correlation ID propagation through `X-Correlation-Id`
- Tenant ID propagation through `X-Tenant-Id`
- Spring Actuator health, metrics, and Prometheus endpoints
- OpenTelemetry OTLP endpoint configured through `OTEL_EXPORTER_OTLP_ENDPOINT`

Production expectations:

- ECS services publish logs to CloudWatch Logs.
- Lambda functions publish logs to CloudWatch Logs.
- OTEL traces are exported to the enterprise collector or AWS managed observability stack.
- Alarms are created for DLQ depth, Lambda errors, ECS unhealthy tasks, Step Functions failures, and API latency.
- Dashboards group signals by service, environment, batch ID, payload type, and tenant.

Correlation contract:

```text
X-Correlation-Id: Required for cross-service tracing. Generated if absent.
X-Causation-Id: Optional event lineage ID.
X-Tenant-Id: Required outside local development.
```
