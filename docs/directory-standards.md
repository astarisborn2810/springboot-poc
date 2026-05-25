# Enterprise Directory Standards

Package naming:

- Root package: `com.pearl.payroll`
- Service packages: `com.pearl.payroll.<domain>`
- Lambda packages: `com.pearl.payroll.lambda.<function>`
- Common packages: `com.pearl.payroll.common.<capability>`
- Do not put service-specific logic in `common-library`.

File naming:

- Spring Boot main classes end with `Application`.
- REST controllers end with `Controller`.
- Configuration classes end with `Configuration`.
- DTOs use nouns and should be immutable records where practical.
- Exceptions end with `Exception`.
- CloudFormation files use `template.yml`.

Docker naming:

- Local images: `pearl/<service-name>:<tag>`
- ECR repositories: `pearl/<service-name>`
- Container names: `<service-name>`
- Dockerfiles live at the module root.

ECS naming:

- Cluster: `pearl-payroll-<env>-cluster`
- Service: `pearl-payroll-<env>-<service-name>`
- Task family: `pearl-payroll-<env>-<service-name>`
- Log group: `/ecs/pearl-payroll/<env>/<service-name>`

CloudFormation naming:

- Stack: `pearl-payroll-<env>-<capability>`
- Parameters use PascalCase.
- Outputs use PascalCase and include the resource type when helpful.
- Every production resource must carry `Application` and `Environment` tags.
