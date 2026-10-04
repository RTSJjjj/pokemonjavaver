@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click Android build (R14): legacy placeholder target
REM
REM  Full pipeline (project3 sections 50, 54, 63): the same 8 steps as the
REM  modern build, with its OWN Gradle module (:android-legacy:assembleDebug).
REM  Usage:
REM     scripts\build-android-legacy.bat "D:\Game\PokemonProject"
REM     scripts\build-android-legacy.bat              (uses builder-config.json)
REM
REM  R13 evidence: the current libGDX backend forces minSdk 21, so this module
REM  is a placeholder (API 21+, real API 14 / ARMv7 work needs the older
REM  backend and is a later milestone). The packaged APK is a Technical
REM  Preview; modern and legacy never share a build configuration, only the
REM  generated game data in generated/.
REM  Outputs go to dist/, generated/ and logs/; the original RMXP project is
REM  never written to.
REM  Exit code: 0 = success, 1 = a build step failed (build stops).
REM ===========================================================================
chcp 65001 >nul

REM scripts\ lives one level below the builder root.
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"
set "BAT=%ROOT%\builder.bat"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"

echo [Pokemon Builder - build-android-legacy]

where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found in PATH. Install Node.js 18 or newer.
  exit /b 1
)
if not exist "%BAT%" (
  echo [ERROR] builder.bat not found: %BAT%
  exit /b 1
)
if not exist "%CLI%" (
  echo [ERROR] Builder CLI not found: %CLI%
  exit /b 1
)
if not exist "%CONFIG%" (
  echo [ERROR] builder-config.json not found: %CONFIG%
  exit /b 1
)

REM The CLI validates, audits, converts, checks the prerequisites and stops at
REM the first failed step; it also creates the output directories and writes
REM logs/latest.log.
call "%BAT%" build-android-legacy %*
exit /b %errorlevel%