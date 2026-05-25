param(
    [string]$ComposeFile = "local/docker-compose/docker-compose.yml"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

docker compose -f $ComposeFile down
