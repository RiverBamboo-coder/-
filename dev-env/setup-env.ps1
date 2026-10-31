# bri-pm dev environment setup (JDK 17 + Maven + optional Aliyun mirror)
# Usage (double-click 设置开发环境.bat, or):
#   powershell -NoProfile -ExecutionPolicy Bypass -File setup-env.ps1
#   powershell ... -File setup-env.ps1 -JdkPath "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot" -MavenPath "C:\maven\apache-maven-3.9.9"
# Only USER-level environment variables are written; no admin rights needed.

param(
    [string]$JdkPath = '',
    [string]$MavenPath = ''
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$text) { Write-Host "[*] $text" -ForegroundColor Cyan }
function Write-Ok([string]$text)   { Write-Host "[OK] $text" -ForegroundColor Green }
function Write-Warn2([string]$text){ Write-Host "[!!] $text" -ForegroundColor Yellow }

function Add-ToUserPath([string]$dir) {
    $old = [Environment]::GetEnvironmentVariable('Path', 'User')
    if (-not $old) { $old = '' }
    $parts = $old -split ';' | Where-Object { $_ -and $_.Trim() -ne '' }
    if ($parts -contains $dir) { return }
    $new = (@($dir) + $parts) -join ';'
    [Environment]::SetEnvironmentVariable('Path', $new, 'User')
}

function Set-JdkEnv([string]$jdkHome) {
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdkHome, 'User')
    Add-ToUserPath (Join-Path $jdkHome 'bin')
    Write-Ok "JAVA_HOME set to $jdkHome"
}

function Find-Jdk {
    $roots = @(
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\BellSoft",
        "$env:ProgramFiles\Zulu",
        "$env:ProgramFiles\Amazon Corretto",
        "C:\java", "D:\java", "D:\develop"
    )
    $found = @()
    foreach ($r in $roots) {
        if (Test-Path $r) {
            $found += Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
                Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') }
        }
    }
    if ($found.Count -eq 0) { return $null }
    $jdk17 = $found | Where-Object { $_.Name -match '17' } | Select-Object -First 1
    if ($jdk17) { return $jdk17.FullName }
    return $found[0].FullName
}

function Find-Maven {
    $roots = @(
        "$env:ProgramFiles\apache-maven*",
        "C:\maven", "D:\maven", "D:\develop", "D:\tools", "C:\tools"
    )
    foreach ($r in $roots) {
        $dirs = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\mvn.cmd') }
        if ($dirs) { return $dirs[0].FullName }
    }
    return $null
}

Write-Host ""
Write-Host "=== bri-pm dev environment setup ===" -ForegroundColor White
Write-Host ""

# ---------- 1. JDK ----------
if ($JdkPath -ne '') {
    $jdk = $JdkPath
} else {
    Write-Step "Looking for JDK (bin\javac.exe) ..."
    $jdk = Find-Jdk
}

if (-not $jdk) {
    Write-Warn2 "JDK not found."
    Write-Host "    1) Download Temurin 17 (Windows x64, .msi): https://adoptium.net/temurin/releases/?version=17"
    Write-Host "    2) Install with default options, then run this script again."
    Write-Host "    Or pass the folder:  -JdkPath 'C:\Program Files\Eclipse Adoptium\jdk-17.x.x.xx-hotspot'"
    Write-Host ""
    exit 1
}
if (-not (Test-Path (Join-Path $jdk 'bin\javac.exe'))) {
    Write-Warn2 "This folder has no bin\javac.exe (it looks like a JRE, not a JDK): $jdk"
    exit 1
}
$javaVersion = & (Join-Path $jdk 'bin\java.exe') -version 2>&1 | Select-Object -First 1
Write-Ok "JDK: $jdk"
Write-Host "     $javaVersion"

# ---------- 2. Maven ----------
if ($MavenPath -ne '') {
    $mvn = $MavenPath
} else {
    Write-Step "Looking for Maven (bin\mvn.cmd) ..."
    $mvn = Find-Maven
}

if (-not $mvn) {
    Write-Warn2 "Maven not found (JDK part will still be configured)."
    Write-Host "    1) Download 'Binary zip archive': https://maven.apache.org/download.cgi  (apache-maven-3.9.x-bin.zip)"
    Write-Host "    2) Unzip to  C:\maven\apache-maven-3.9.x   (avoid spaces in the path)"
    Write-Host "    3) Run this script again, or:  -MavenPath 'C:\maven\apache-maven-3.9.9'"
    Write-Host "    Tip: IntelliJ IDEA bundles Maven, so the command-line mvn is optional."
    Write-Host ""
    Set-JdkEnv $jdk
    Write-Host "Open a NEW terminal for JAVA_HOME to take effect."
    Write-Host ""
    exit 2
}
if (-not (Test-Path (Join-Path $mvn 'bin\mvn.cmd'))) {
    Write-Warn2 "This folder has no bin\mvn.cmd : $mvn"
    exit 1
}
Write-Ok "Maven: $mvn"

# ---------- 3. Persist environment variables ----------
Write-Step "Writing user environment variables ..."
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
[Environment]::SetEnvironmentVariable('MAVEN_HOME', $mvn, 'User')
Add-ToUserPath (Join-Path $jdk 'bin')
Add-ToUserPath (Join-Path $mvn 'bin')
Write-Ok "JAVA_HOME / MAVEN_HOME / PATH written (User scope)."

# ---------- 4. Maven mirror (only when settings.xml is absent) ----------
$m2dir = Join-Path $env:USERPROFILE '.m2'
$settings = Join-Path $m2dir 'settings.xml'
if (-not (Test-Path $settings)) {
    Write-Step "Creating ~/.m2/settings.xml with Aliyun mirror ..."
    New-Item -ItemType Directory -Force -Path $m2dir | Out-Null
    $xml = @'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Central</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
'@
    Set-Content -Path $settings -Value $xml -Encoding UTF8
    Write-Ok "settings.xml created: $settings"
} else {
    Write-Ok "settings.xml already exists, left untouched."
}

# ---------- 5. Verify ----------
Write-Host ""
Write-Step "Verifying ..."
& (Join-Path $jdk 'bin\java.exe') -version 2>&1 | ForEach-Object { Write-Host "    $_" }
& (Join-Path $jdk 'bin\javac.exe') -version 2>&1 | ForEach-Object { Write-Host "    $_" }
& (Join-Path $mvn 'bin\mvn.cmd') -v 2>&1 | Select-Object -First 3 | ForEach-Object { Write-Host "    $_" }

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
Write-Host "Open a NEW PowerShell window (PATH refreshes only there), then run:"
Write-Host "    cd D:\数据库三级项目\bri-pm"
Write-Host "    mvn clean test"
Write-Host ""
