# CI/CD Placeholders

GitHub Actions:

- `.github/workflows/ci.yml` verifies the Maven reactor on pull requests and pushes.
- `.github/workflows/deploy-ecs-placeholder.yml` marks the ECS deployment stages.
- `.github/workflows/package-lambda-placeholder.yml` packages Java Lambda artifacts.

Jenkins:

- `Jenkins/Jenkinsfile` builds and verifies the monorepo.
- `Jenkins/Jenkinsfile.deploy` defines environment promotion stages.

Enterprise gates to add:

- SAST
- SCA and dependency license checks
- Secret scanning
- Container image scanning
- SBOM generation
- Change management approval
- Production deployment freeze checks
- Automated rollback checks
