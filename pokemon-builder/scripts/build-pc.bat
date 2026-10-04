@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click PC build (R14)
REM
REM  Full pipeline (project3 sections 45, 63):
REM    [1/8] Audit              validate + scan data/events/scripts + reports
REM    [2/8] Convert Maps       scan the RMXP data, write the map IR
REM    [3/8] Compile Events     the same converter writes the event IR
REM    [4/8] Translate Scripts  generated/scripts/ir.json
REM    [5/8] Optimize Audio     generated/audio + runtime manifest
REM    [6/8] Validate Runtime Data
REM    [7/8] Gradle Build       runtime tests (agentCheck) + lwjgl3:dist
REM    [8/8] Package            verify dist/ artifacts + build report
REM
REM  Usage:
REM     scripts\build-pc.bat "D:\Game\PokemonProject"
REM     scripts\build-pc.bat                (uses builder-config.json)
REM     scripts\build-pc.bat --project "D:\Game\PokemonProject"
REM     scripts\build-pc.bat --runtime-image "C:\jre"   (JDK without jmods/)
REM
REM  Outputs: pokemon-builder/dist/PokemonGame.jar and dist/Windows/
REM  (PokemonGame.exe + app/ + runtime/ + runtime-data/), plus
REM  generated/build-report.json (project3 section 55). The original RMXP
REM  project is never written to.
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

echo [Pokemon Builder - build-pc]

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

REM The CLI validates, audits, converts, checks the runtime and stops at the
REM first failed step; it also creates the output directories and writes
REM logs/latest.log.
call "%BAT%" build-pc %*
exit /b %errorlevel%
