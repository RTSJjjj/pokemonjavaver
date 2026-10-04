@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click entry (Phase 2)
REM  Delegates every command to builder\src\cli.js (Node.js).
REM  Usage: builder.bat audit "D:\Game\PokemonProject"
REM ===========================================================================
chcp 65001 >nul

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"

echo [Pokemon Builder]

where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found in PATH. Install Node.js 18 or newer.
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

REM Auto-create output directories.
for %%D in (logs build generated dist) do (
  if not exist "%ROOT%\%%D" (
    mkdir "%ROOT%\%%D" >nul 2>&1
    if errorlevel 1 (
      echo [ERROR] Cannot create directory: %ROOT%\%%D
      exit /b 1
    )
  )
)

if "%~1"=="" (
  node "%CLI%" help
  exit /b %errorlevel%
)

node "%CLI%" %*
exit /b %errorlevel%