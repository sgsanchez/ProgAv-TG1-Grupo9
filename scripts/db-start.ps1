# Inicia el servidor PostgreSQL portable del proyecto (sin Docker, sin administrador).
# Uso:  .\scripts\db-start.ps1

$ErrorActionPreference = 'Stop'

$pgDir   = Join-Path $env:LOCALAPPDATA 'PostgreSQL\pgsql'
$dataDir = if ($env:PGDATA_VENTAS) { $env:PGDATA_VENTAS } else { 'D:\pgdata-ventas' }
$logFile = Join-Path $pgDir '..\postgres.log'

if (-not (Test-Path (Join-Path $pgDir 'bin\pg_ctl.exe'))) {
    Write-Error "No se encontró PostgreSQL en $pgDir. Ejecute la instalación portable indicada en el README."
}

if (-not (Test-Path $dataDir)) {
    Write-Error "No se encontró el clúster de datos en $dataDir."
}

& (Join-Path $pgDir 'bin\pg_ctl.exe') status -D $dataDir *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "PostgreSQL ya está en ejecución (puerto 5432)." -ForegroundColor Yellow
    exit 0
}

& (Join-Path $pgDir 'bin\pg_ctl.exe') start -D $dataDir -l $logFile -o "-p 5432" -w
if ($LASTEXITCODE -ne 0) {
    Write-Error "No se pudo iniciar PostgreSQL. Revise el log: $logFile"
}

Write-Host "PostgreSQL iniciado en localhost:5432 (base de datos: ventasdb)." -ForegroundColor Green
