# Verificacion del entorno al encender el equipo.
# Uso:  .\scripts\verificar-entorno.ps1
#       .\scripts\verificar-entorno.ps1 -Compilar    (ademas compila y ejecuta pruebas)

param(
    [switch]$Compilar
)

$ErrorActionPreference = 'Continue'

$pgDir   = Join-Path $env:LOCALAPPDATA 'PostgreSQL\pgsql'
$dataDir = if ($env:PGDATA_VENTAS) { $env:PGDATA_VENTAS } else { 'D:\pgdata-ventas' }
$gitExe  = 'C:\Program Files\Git\cmd\git.exe'
$repo    = Split-Path -Parent $PSScriptRoot
$ok      = $true

Write-Host "=== 1. Herramientas ===" -ForegroundColor Cyan
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) { Write-Host "  Java: $((& java -version 2>&1 | Select-Object -First 1))" -ForegroundColor Green }
else { Write-Host "  Java NO encontrado en el PATH" -ForegroundColor Red; $ok = $false }

if (Test-Path $gitExe) { Write-Host "  Git: $(& $gitExe --version)" -ForegroundColor Green }
else { Write-Host "  Git NO encontrado" -ForegroundColor Red; $ok = $false }

if (Test-Path (Join-Path $pgDir 'bin\postgres.exe')) { Write-Host "  PostgreSQL: $(& (Join-Path $pgDir 'bin\postgres.exe') --version)" -ForegroundColor Green }
else { Write-Host "  PostgreSQL NO encontrado en $pgDir" -ForegroundColor Red; $ok = $false }

Write-Host "=== 2. Base de datos ===" -ForegroundColor Cyan
if (Test-Path (Join-Path $pgDir 'bin\pg_ctl.exe')) {
    & (Join-Path $pgDir 'bin\pg_ctl.exe') status -D $dataDir *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Servidor ya en ejecucion" -ForegroundColor Green
    } else {
        Write-Host "  Iniciando servidor..."
        & (Join-Path $PSScriptRoot 'db-start.ps1')
    }
    $env:PGPASSWORD = 'ventas123'
    $check = & (Join-Path $pgDir 'bin\psql.exe') -h localhost -p 5432 -U ventas -d ventasdb -t -A -c "SELECT 1;" 2>&1
    if ($LASTEXITCODE -eq 0 -and ($check | Out-String).Trim() -eq '1') {
        Write-Host "  Conexion a ventasdb: OK" -ForegroundColor Green
    } else {
        Write-Host "  Conexion a ventasdb: FALLO -> $check" -ForegroundColor Red
        $ok = $false
    }
}

Write-Host "=== 3. Repositorio ===" -ForegroundColor Cyan
if (Test-Path $gitExe) {
    Write-Host "  Rama actual: $(& $gitExe -C $repo rev-parse --abbrev-ref HEAD)"
    $pending = & $gitExe -C $repo status --short
    if ($pending) {
        Write-Host "  Cambios sin confirmar:" -ForegroundColor Yellow
        $pending | ForEach-Object { Write-Host "    $_" }
    } else {
        Write-Host "  Arbol de trabajo limpio" -ForegroundColor Green
    }
    & $gitExe -C $repo log --oneline -3 | ForEach-Object { Write-Host "    $_" }
}

if ($Compilar) {
    Write-Host "=== 4. Compilacion y pruebas ===" -ForegroundColor Cyan
    Push-Location $repo
    & '.\mvnw.cmd' -B -ntp clean verify
    if ($LASTEXITCODE -eq 0) { Write-Host "  BUILD SUCCESS" -ForegroundColor Green }
    else { Write-Host "  BUILD FAILURE" -ForegroundColor Red; $ok = $false }
    Pop-Location
}

Write-Host ""
if ($ok) { Write-Host "Entorno listo para continuar." -ForegroundColor Green }
else { Write-Host "Revisar los puntos marcados en rojo." -ForegroundColor Red }
