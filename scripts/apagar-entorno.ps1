# Apagado seguro del entorno de desarrollo.
# Uso:  .\scripts\apagar-entorno.ps1
#
# Detiene la aplicacion y PostgreSQL, y avisa si quedaron cambios sin confirmar en Git.

$ErrorActionPreference = 'Continue'

$pgDir   = Join-Path $env:LOCALAPPDATA 'PostgreSQL\pgsql'
$dataDir = if ($env:PGDATA_VENTAS) { $env:PGDATA_VENTAS } else { 'D:\pgdata-ventas' }
$gitExe  = 'C:\Program Files\Git\cmd\git.exe'
$repo    = Split-Path -Parent $PSScriptRoot

Write-Host "=== 1. Aplicacion Java ===" -ForegroundColor Cyan
$procs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
         Where-Object { $_.CommandLine -like '*ventas-facturacion*' }
if ($procs) {
    foreach ($p in $procs) {
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "  Detenida instancia PID $($p.ProcessId)"
    }
} else {
    Write-Host "  No habia instancias de la API en ejecucion"
}

Write-Host "=== 2. PostgreSQL ===" -ForegroundColor Cyan
$pgCtl = Join-Path $pgDir 'bin\pg_ctl.exe'
if (Test-Path $pgCtl) {
    & $pgCtl status -D $dataDir *> $null
    if ($LASTEXITCODE -eq 0) {
        & $pgCtl stop -D $dataDir -m fast -w
        Write-Host "  PostgreSQL detenido (datos conservados en $dataDir)" -ForegroundColor Green
    } else {
        Write-Host "  PostgreSQL ya estaba detenido"
    }
} else {
    Write-Host "  Binarios de PostgreSQL no encontrados en $pgDir" -ForegroundColor Yellow
}

Write-Host "=== 3. Estado de Git ===" -ForegroundColor Cyan
if (Test-Path $gitExe) {
    $pending = & $gitExe -C $repo status --short
    if ($pending) {
        Write-Host "  ATENCION: hay cambios SIN confirmar:" -ForegroundColor Yellow
        $pending | ForEach-Object { Write-Host "    $_" }
        Write-Host "  Confirme con: git add -A ; git commit -m `"wip: avance parcial`"" -ForegroundColor Yellow
    } else {
        Write-Host "  Sin cambios pendientes" -ForegroundColor Green
    }
    Write-Host "  Ultimos commits:"
    & $gitExe -C $repo log --oneline -3 | ForEach-Object { Write-Host "    $_" }
} else {
    Write-Host "  Git no encontrado" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Equipo listo para apagarse." -ForegroundColor Green
