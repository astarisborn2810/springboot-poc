param(
    [string]$ImageTag = "local"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

$Services = @(
    "financial-processing-service",
    "indicative-processing-service",
    "result-tracking-service",
    "batch-completion-service",
    "config-service"
)

mvn -DskipTests package

foreach ($Service in $Services) {
    docker build `
        -t "pearl/$Service`:$ImageTag" `
        -f "services/$Service/Dockerfile" `
        "services/$Service"
}
