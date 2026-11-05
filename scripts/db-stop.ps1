# Detiene el servidor PostgreSQL portable del proyecto.
# Uso:  .\scripts\db-stop.ps1

$ErrorActionPreference = 'Stop'

$pgDir   = Join-Path $env:LOCALAPPDATA 'PostgreSQL\pgsql'
$dataDir = if ($env:PGDATA_VENTAS) { $env:PGDATA_VENTAS } else { 'D:\pgdata-ventas' }

if (-not (Test-Path (Join-Path $pgDir 'bin\pg_ctl.exe'))) {
    Write-Error "No se encontró PostgreSQL en $pgDir."
}

& (Join-Path $pgDir 'bin\pg_ctl.exe') stop -D $dataDir -m fast -w
Write-Host "PostgreSQL detenido." -ForegroundColor Green
