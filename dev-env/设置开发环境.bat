@echo off
rem Double-click this file after installing JDK 17 (and Maven if you want command-line mvn).
rem It calls setup-env.ps1 with ExecutionPolicy Bypass, so no PowerShell settings are changed.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-env.ps1"
echo.
pause
