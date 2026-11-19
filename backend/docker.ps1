[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("up", "down", "logs", "rebuild", "shell", "ps", "config")]
    [string]$Action,

    [Parameter(Position = 1)]
    [string]$Service
)

$ErrorActionPreference = "Stop"
$envFile = Join-Path $PSScriptRoot ".env"
$composeFile = Join-Path $PSScriptRoot "docker-compose.yml"

if (-not (Test-Path $envFile)) {
    throw "Falta backend/.env. Copia backend/.env.example y define secretos antes de iniciar."
}

$composeArgs = @(
    "compose",
    "--env-file", $envFile,
    "--project-directory", $PSScriptRoot,
    "-f", $composeFile
)

switch ($Action) {
    "up" {
        & docker @composeArgs up -d --build
    }
    "down" {
        & docker @composeArgs down
    }
    "logs" {
        if ($Service) {
            & docker @composeArgs logs --tail=100 -f $Service
        } else {
            & docker @composeArgs logs --tail=100 -f
        }
    }
    "rebuild" {
        & docker @composeArgs build --no-cache
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
        & docker @composeArgs up -d
    }
    "shell" {
        if (-not $Service) {
            $Service = "auth-service"
        }
        & docker @composeArgs exec $Service sh
    }
    "ps" {
        & docker @composeArgs ps
    }
    "config" {
        & docker @composeArgs config
    }
}

exit $LASTEXITCODE
