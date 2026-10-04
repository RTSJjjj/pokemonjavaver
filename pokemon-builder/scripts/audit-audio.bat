@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click audio audit (audio phase A1)
REM
REM  Runs the read only audio scan through the Builder CLI:
REM    1. validate the project
REM    2. scan Audio/BGM/BGS/ME/SE (+ extra categories), detect codecs from
REM       file content, hash every file, collect duplicates and problems
REM    3. write build/reports/audio-audit.json and docs/audio-audit.md
REM
REM  Usage:
REM     scripts\audit-audio.bat "D:\Game\PokemonProject"
REM     scripts\audit-audio.bat                (uses builder-config.json)
REM     scripts\audit-audio.bat --project "D:\Game\PokemonProject"
REM     scripts\audit-audio.bat --preset high
REM
REM  The Audio/ tree is an input only: the original RMXP project is never
REM  written to. Exit code: 0 = audit succeeded, 1 = a stage failed.
REM ===========================================================================
chcp 65001 >nul

REM scripts\ lives one level below the builder root.
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"
set "BAT=%ROOT%\builder.bat"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"

echo [Pokemon Builder - audit-audio]

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

REM The CLI validates the project, creates output directories and stops the
REM pipeline as soon as one stage fails, so no extra checks are needed here.
call "%BAT%" audio-audit %*
exit /b %errorlevel%
