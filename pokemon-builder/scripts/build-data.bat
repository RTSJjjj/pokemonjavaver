@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click data build (Phase 8)
REM
REM  Runs the data pipeline through the Builder CLI, in order:
REM    1. validate the project          (Data/, Game.rxproj, PBS/, Graphics/)
REM    2. scan the project              (Data/*.rxdata + Scripts.rxdata + events)
REM    3. convert to intermediate data   (JSON Debug IR in generated/)
REM
REM  Usage:
REM     scripts\build-data.bat "D:\Game\PokemonProject"
REM     scripts\build-data.bat                (uses builder-config.json)
REM     scripts\build-data.bat --project "D:\Game\PokemonProject"
REM
REM  Outputs (never inside the source project):
REM     generated/project.json, maps/, events/, common-events/,
REM     scripts/, metadata/
REM  Markdown audit reports are produced by scripts\audit-project.bat.
REM  The project path is an input only: the original RMXP project is never
REM  written to.
REM  Exit code: 0 = data build succeeded, 1 = a step failed (build stops).
REM ===========================================================================
chcp 65001 >nul

REM scripts\ lives one level below the builder root.
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"
set "BAT=%ROOT%\builder.bat"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"

echo [Pokemon Builder - build-data]

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

REM The CLI validates, scans, converts and stops at the first failed step; it
REM also creates the output directories and writes logs/latest.log.
call "%BAT%" build-data %*
exit /b %errorlevel%