@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click Android build (R14): modern target (API 21+)
REM
REM  Full pipeline (project3 sections 54, 63):
REM    [1/8] Audit / [2/8] Convert Maps / [3/8] Compile Events
REM    [4/8] Translate Scripts / [5/8] Optimize Audio
REM    [6/8] Validate Runtime Data
REM    [7/8] Gradle Android build (android:assembleDebug)
REM    [8/8] Package dist\PokemonGame-Android.apk (debug APK, Technical Preview)
REM  Usage:
REM     scripts\build-android.bat "D:\Game\PokemonProject"
REM     scripts\build-android.bat                (uses builder-config.json)
REM     scripts\build-android.bat --project "D:\Game\PokemonProject"
REM
REM  Modern target: API 21+, ABIs armeabi-v7a / arm64-v8a / x86 / x86_64.
REM  This target uses its OWN Gradle module (:android:assembleDebug); it never
REM  shares a build configuration with the legacy target. The stage 2 Android
REM  runtime is not feature complete, so the packaged APK is a Technical
REM  Preview - but the pipeline really produces it (no skipped packaging).
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

echo [Pokemon Builder - build-android]

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
call "%BAT%" build-android %*
exit /b %errorlevel%