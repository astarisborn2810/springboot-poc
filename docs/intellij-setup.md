# IntelliJ Setup

1. Install JDK 24 and set `JAVA_HOME`.
2. Open IntelliJ IDEA.
3. Select `File > Open` and choose `D:\office-work\Empwr\Code\pearl-payroll-platform\pom.xml`.
4. Import as a Maven project.
5. Set Project SDK to Java 24 under `File > Project Structure > Project`.
6. Enable annotation processing under `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`.
7. Set Maven runner JRE to Java 24 under `Settings > Build Tools > Maven > Runner`.
8. Install or enable these plugins:
   - Docker
   - AWS Toolkit
   - GitHub
   - Maven
9. Mark generated folders as excluded only if IntelliJ does not already exclude `target/`.
10. Use the Maven tool window to run `clean verify` from the root aggregator.

Recommended run configurations:

| Service | Main Class | Default Port |
| --- | --- | --- |
| financial-processing-service | `com.pearl.payroll.financial.FinancialProcessingServiceApplication` | 8081 |
| indicative-processing-service | `com.pearl.payroll.indicative.IndicativeProcessingServiceApplication` | 8082 |
| result-tracking-service | `com.pearl.payroll.resulttracking.ResultTrackingServiceApplication` | 8083 |
| batch-completion-service | `com.pearl.payroll.batchcompletion.BatchCompletionServiceApplication` | 8084 |
| config-service | `com.pearl.payroll.config.ConfigServiceApplication` | 8085 |

Use environment variables from `.env.example` for local runs.
