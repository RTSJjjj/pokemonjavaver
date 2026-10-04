@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click build cleanup (Phase 11)
REM
REM  Removes the build outputs produced by this builder:
REM     build/            (reports/, cache.json)
REM     generated/        (JSON Debug IR)
REM     logs/build-temp/  (temporary build files)
REM
REM  It never touches the source RMXP project, docs/ or dist/. Pass --all to
REM  also remove dist/ and the archived logs/*.log (the current log file is
REM  kept so this run stays readable).
REM
REM  Usage:
REM     scripts\clean-build.bat
REM     scripts\clean-build.bat --all
REM
REM  A project path is not needed: nothing here reads the project.
REM  Exit code: 0 = cleanup succeeded, 1 = a step failed (the build stops).
REM ===========================================================================
chcp 65001 >nul

REM scripts\ lives one level below the builder root.
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"
set "BAT=%ROOT%\builder.bat"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"

echo [Pokemon Builder - clean-build]

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

REM The CLI refuses to clean anything outside the builder root (isInside guard),
REM so the source project, docs/ and dist/ are never removed unless --all is
REM given for dist/. It stops at the first failed step and returns a non-zero
REM exit code, which this wrapper passes on unchanged.
call "%BAT%" clean %*
exit /b %errorlevel%