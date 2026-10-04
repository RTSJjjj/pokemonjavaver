@echo off
setlocal EnableExtensions
REM ===========================================================================
REM  pokemon-builder - local CI chain (project3 section 62)
REM
REM    [1/5] Builder Tests   node --test builder/tests/*.test.js   (whole suite)
REM    [2/5] Compiler Tests  script compiler / data converter / event analyzer /
REM                          fixture-project (the compiler + golden subset)
REM    [3/5] Core Tests      gradlew :core:test :core:auditCoreDependencies
REM    [4/5] Desktop Build   gradlew lwjgl3:dist  (Runnable JAR + dist\Windows)
REM    [5/5] Android Debug   gradlew android:assembleDebug (+legacy) when the
REM                          Android SDK is available, otherwise SKIPPED
REM
REM  Windows only. The Gradle stages run through an ASCII subst drive when the
REM  checkout path is not ASCII: Gradle's test worker @argfile is written in the
REM  daemon encoding but read in the native encoding, which breaks every test
REM  class on a Chinese path (see docs/desktop-build.md).
REM
REM  Desktop packaging with a JDK without jmods/: set POKEMON_RUNTIME_IMAGE to
REM  the runtime directory and stage 4 passes -PruntimeImage to jpackage.
REM
REM  Usage: scripts\ci-build.bat
REM  Exit code: 0 = everything that could run passed, 1 = a stage failed.
REM ===========================================================================
chcp 65001 >nul

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
for %%I in ("%ROOT%\..") do set "ROOT=%%~fI"

if not exist "%ROOT%\logs" mkdir "%ROOT%\logs" >nul 2>nul
set "LOG=%ROOT%\logs\ci-build.log"

echo [pokemon-builder CI] Builder -^> Compiler -^> Core -^> Desktop -^> Android
echo [pokemon-builder CI] checkout: %ROOT%

REM ---------------------------------------------------------------------------
REM Run from an ASCII drive view (Y/Z/W/V). subst is available to normal users
REM and the drive disappears at the end.
REM ---------------------------------------------------------------------------
set "RUNROOT=%ROOT%"
for %%D in (Y Z W V) do (
  if not defined SUBST_DRIVE if not exist "%%D:\" (
    subst %%D: "%ROOT%" >nul 2>nul
    if not errorlevel 1 set "SUBST_DRIVE=%%D:"
  )
)
if defined SUBST_DRIVE (
  set "RUNROOT=%SUBST_DRIVE%"
  echo [pokemon-builder CI] running from %SUBST_DRIVE% ^(ASCII view^)
)

echo =========================================================================== >> "%LOG%"
echo [%DATE% %TIME%] CI chain from %ROOT% >> "%LOG%"

echo.
echo [1/5] Builder Tests
call "%RUNROOT%\scripts\test-builder.bat"
if errorlevel 1 goto :failed

echo.
echo [2/5] Compiler Tests
pushd "%RUNROOT%"
node --test builder\tests\script-compiler.test.js builder\tests\data-converter.test.js builder\tests\event-analyzer.test.js builder\tests\fixture-project.test.js
if errorlevel 1 (popd & goto :failed)
popd

echo.
echo [3/5] Core Tests
pushd "%RUNROOT%\runtime"
call gradlew.bat core:test core:auditCoreDependencies --console=plain
if errorlevel 1 (popd & goto :failed)
popd

echo.
echo [4/5] Desktop Build
set "RUNTIME_IMAGE_ARG="
if defined POKEMON_RUNTIME_IMAGE set "RUNTIME_IMAGE_ARG=-PruntimeImage=%POKEMON_RUNTIME_IMAGE%"
pushd "%RUNROOT%\runtime"
call gradlew.bat lwjgl3:dist %RUNTIME_IMAGE_ARG% --console=plain
if errorlevel 1 (popd & goto :failed)
popd

echo.
echo [5/5] Android Debug Build
set "ANDROID_PRESENT="
if defined ANDROID_HOME set "ANDROID_PRESENT=1"
if defined ANDROID_SDK_ROOT set "ANDROID_PRESENT=1"
if exist "%RUNROOT%\runtime\local.properties" set "ANDROID_PRESENT=1"
if not defined ANDROID_PRESENT (
  echo       SKIPPED: Android SDK not found ^(ANDROID_HOME / ANDROID_SDK_ROOT / local.properties^).
  echo       Open the project once in Android Studio or set ANDROID_HOME to include this stage.
) else (
  pushd "%RUNROOT%\runtime"
  call gradlew.bat android:assembleDebug android-legacy:assembleDebug --console=plain
  if errorlevel 1 (popd & goto :failed)
  popd
)

echo.
echo CI BUILD SUCCESS
echo [%DATE% %TIME%] CI BUILD SUCCESS >> "%LOG%"
set "CODE=0"
goto :finish

:failed
echo.
echo CI BUILD FAILED
echo [%DATE% %TIME%] CI BUILD FAILED >> "%LOG%"
set "CODE=1"

:finish
if defined SUBST_DRIVE subst /d %SUBST_DRIVE% >nul 2>nul
exit /b %CODE%
