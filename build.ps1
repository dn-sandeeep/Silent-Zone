#!/usr/bin/env pwsh
# SilentZone Build Helper
# Usage: .\build.ps1
# 
# Yeh script Android Studio ke Gradle daemon se conflict avoid karta hai
# aur R.jar lock problem ko automatically fix karta hai.

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  SilentZone Build Helper" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Step 1: Gradle daemons band karo
Write-Host "`n[1/4] Stopping Gradle daemons..." -ForegroundColor Yellow
.\gradlew --stop 2>&1 | Out-Null
Start-Sleep -Seconds 2

# Step 2: R.jar delete karo (agar lock nahi hai)
$rjar = ".\app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\debug\processDebugResources\R.jar"
Write-Host "[2/4] Clearing locked R.jar..." -ForegroundColor Yellow
if (Test-Path $rjar) {
    Remove-Item -Force $rjar -ErrorAction SilentlyContinue
    if (Test-Path $rjar) {
        Write-Host "       R.jar is locked by another process. Attempting force kill..." -ForegroundColor Red
        # Kill all java processes that might be locking it
        Get-Process | Where-Object { $_.Name -eq "java" } | ForEach-Object {
            Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 3
        Remove-Item -Force $rjar -ErrorAction SilentlyContinue
        if (Test-Path $rjar) {
            Write-Host "       WARNING: Could not delete R.jar! Close Android Studio first." -ForegroundColor Red
            Write-Host "       Trying to build anyway..." -ForegroundColor Yellow
        } else {
            Write-Host "       R.jar cleared!" -ForegroundColor Green
        }
    } else {
        Write-Host "       R.jar cleared!" -ForegroundColor Green
    }
} else {
    Write-Host "       R.jar not present - OK" -ForegroundColor Green
}

# Step 3: Build
Write-Host "[3/4] Starting build..." -ForegroundColor Yellow
$buildOutput = .\gradlew assembleDebug 2>&1

# Step 4: Result
Write-Host "[4/4] Build result:" -ForegroundColor Yellow
if ($LASTEXITCODE -eq 0) {
    Write-Host "`n  BUILD SUCCESSFUL!" -ForegroundColor Green
    $apk = Get-ChildItem ".\app\build\outputs\apk\debug\*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($apk) {
        Write-Host "  APK: $($apk.FullName)" -ForegroundColor Green
        Write-Host "  Size: $([math]::Round($apk.Length / 1MB, 2)) MB" -ForegroundColor Green
    }
} else {
    Write-Host "`n  BUILD FAILED!" -ForegroundColor Red
    # Show only errors
    $buildOutput | Where-Object { $_ -match "^e:|error:" } | ForEach-Object {
        Write-Host "  $_" -ForegroundColor Red
    }
}

Write-Host "`n============================================" -ForegroundColor Cyan
