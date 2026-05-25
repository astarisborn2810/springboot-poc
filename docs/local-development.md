# Local Development

Prerequisites:

- Windows 11 or Windows Server developer workstation
- Git in PATH
- JDK 24
- Maven 3.9+
- Docker Desktop with Linux containers
- AWS CLI v2
- Optional: `awslocal` for LocalStack convenience

Start local dependencies:

```powershell
Copy-Item .env.example .env
.\scripts\start-local.ps1
```

Build everything:

```powershell
mvn clean verify
```

Run a service:

```powershell
mvn -pl services/financial-processing-service -am spring-boot:run
```

Smoke check:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8081/v1/financial/status
```

Stop local dependencies:

```powershell
.\scripts\stop-local.ps1
```

Testing strategy:

- Unit tests live next to each module under `src/test/java`.
- Contract tests should validate JSON payload schemas and service boundaries.
- Integration tests should use Testcontainers or LocalStack-backed tests.
- End-to-end tests should execute the Step Functions state machine against isolated test resources.
