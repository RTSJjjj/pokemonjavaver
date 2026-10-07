param([string]$JavaHome = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta')
$ErrorActionPreference = 'Stop'
$builderRoot = Split-Path $PSScriptRoot -Parent
$reportRoot = Join-Path $builderRoot 'logs/p4-static'
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
# This is source compilation only. No Gradle, JVM test execution, game or GUI.
$cacheRoot = Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1'
$jarFiles = Get-ChildItem -LiteralPath $cacheRoot -Recurse -Filter '*.jar' |
    Where-Object { $_.FullName -match 'com\.badlogicgames\.gdx|org\.lwjgl|org\.junit|org\.opentest4j|org\.apiguardian' }
if (-not $jarFiles) { throw 'Cached compilation dependencies are unavailable.' }
$classPath = ($jarFiles.FullName -join ';')
$out = Join-Path $reportRoot 'classes'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$sources = @(
    (Join-Path $builderRoot 'runtime/core/src/main/java'),
    (Join-Path $builderRoot 'runtime/core/src/test/java'),
    (Join-Path $builderRoot 'runtime/lwjgl3/src/main/java')
) | ForEach-Object { Get-ChildItem -LiteralPath $_ -Recurse -Filter '*.java' }
$arguments = @('-encoding', 'UTF-8', '--release', '17', '-classpath', ('"' + $classPath.Replace('\','/') + '"'), '-d', ('"' + $out.Replace('\','/') + '"'))
$arguments += $sources.FullName | ForEach-Object { '"' + $_.Replace('\','/') + '"' }
$argFile = Join-Path $reportRoot 'javac-args.txt'
[IO.File]::WriteAllLines($argFile, $arguments, [Text.UTF8Encoding]::new($false))
& (Join-Path $JavaHome 'bin/javac.exe') ('@' + $argFile) 2>&1 | Tee-Object -FilePath (Join-Path $reportRoot 'javac.log')
if ($LASTEXITCODE -ne 0) { throw 'Java static compilation failed.' }
Write-Output ('JAVA STATIC COMPILE OK: ' + $sources.Count + ' source files')
