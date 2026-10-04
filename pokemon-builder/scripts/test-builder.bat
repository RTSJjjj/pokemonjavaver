@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - one-click automatic test runner (Phase 12)
REM
REM  Runs the whole Builder test suite with node:test:
REM    Builder unit tests     builder\tests\cli.test.js, runtime.test.js
REM    Parser tests           builder\tests\marshal.test.js
REM    Event scanner tests    builder\tests\event-analyzer.test.js
REM    Script scanner tests   builder\tests\scanner.test.js,
REM                            builder\tests\script-analyzer.test.js
REM    Path handling tests    builder\tests\path-handling.test.js
REM    Wrapper / .bat tests   builder\tests\scripts.test.js,
REM                            builder\tests\test-builder.test.js
REM
REM  Usage:
REM     scripts\test-builder.bat
REM         Run the whole suite (recommended before every commit).
REM     scripts\test-builder.bat --test-name-pattern=spaces
REM         Extra arguments are handed to "node --test" unchanged, e.g.
REM         --test-name-pattern=<regex> to run a subset, or a file path
REM         like builder\tests\cli.test.js to run one file. The wrapper
REM         always runs from the builder root, so paths stay relative to it.
REM
REM  Outputs: console only; the suite never reads or writes the source RMXP
REM  project. A test file that fails stops nothing here (this IS the check):
REM  the exit code is 1 whenever any test fails, and 0 only when all pass.
REM ===========================================================================
chcp 65001 >nul

REM scripts\ lives one level below the builder root.
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"
set "BAT=%ROOT%\builder.bat"
set "CLI=%ROOT%\builder\src\cli.js"
set "CONFIG=%ROOT%\builder-config.json"
set "TESTS=%ROOT%\builder\tests"

echo [Pokemon Builder - test-builder]

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
if not exist "%TESTS%" (
  echo [ERROR] Builder tests directory not found: %TESTS%
  exit /b 1
)

cd /d "%ROOT%"
if errorlevel 1 (
  echo [ERROR] Cannot enter builder root: %ROOT%
  exit /b 1
)

REM Extra arguments (node:test options or file paths) are optional. Without
REM them the whole suite runs; with them "node --test" gets them untouched.
set "ARGS=%*"
if not defined ARGS (
  echo [1/1] Running Builder test suite...
  node --test "builder/tests/*.test.js"
) else (
  echo [1/1] Running Builder test suite with options: %ARGS%
  node --test %ARGS%
)

REM node --test exits non-zero as soon as one test fails; that exit code is
REM reported unchanged, so a failing suite can never look like a success.
set "STATUS=%errorlevel%"
if not "%STATUS%"=="0" (
  echo TEST FAILED
  echo [ERROR] the Builder test suite reported failures ^(exit code %STATUS%^).
) else (
  echo TEST SUCCESS
)
exit /b %STATUS%
