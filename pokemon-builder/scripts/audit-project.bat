@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click project audit (Phase 7)
REM
REM  Runs the whole audit pipeline through the Builder CLI, in order:
REM    1. scan the project          (Data/*.rxdata + Scripts.rxdata)
REM    2. scan the Ruby scripts     (zlib sections, decoded, counted)
REM    3. scan the events           (MapXXX.rxdata / CommonEvents.rxdata,
REM                                   commands 355 + 655 merged into Ruby blocks)
REM    4. analyze the Essentials API (categories + unique argument patterns)
REM    5. analyze third party plugins (plugin-only definitions)
REM    6. write the audit reports   (Markdown for humans, JSON for the tools)
REM
REM  Usage:
REM     scripts\audit-project.bat "D:\Game\PokemonProject"
REM     scripts\audit-project.bat                (uses builder-config.json)
REM     scripts\audit-project.bat --project "D:\Game\PokemonProject"
REM
REM  The project path is an input only: the original RMXP project is never
REM  written to. Outputs go to docs/ and build/reports/.
REM  Exit code: 0 = audit succeeded, 1 = a stage failed (build stops at once).
REM ===========================================================================
chcp 65001 >nul

REM scripts\ lives one level below the builder root.
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"
set "BAT=%ROOT%\builder.bat"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"

echo [Pokemon Builder - audit-project]

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
call "%BAT%" audit %*
exit /b %errorlevel%