# Reads Pokemon out of a Ruby Game.rxdata (or a save of this runtime, *.json) and writes them as this runtime converts them,
# next to the raw values of the file, so a transfer can be checked against the original game. Gradle-free (plain javac).
# Usage: powershell -File scripts\legacy-save-probe.ps1 -Saves <file[;file...]> -Out <result.txt> [-Boxes party,100,200 | all | stats] [-Data <generated dir>]
#   -Boxes: comma list of "party" and box numbers (1 based); "all" for everything; "stats" for totals only (also compares the
#           stored stats with the derived ones, which shows whether species / nature / IVs / EVs / level were read correctly).
param([Parameter(Mandatory)][string]$Saves, [Parameter(Mandatory)][string]$Out, [string]$Boxes = 'party,100,200', [string]$Data = '')
$ErrorActionPreference = 'Stop'
$java = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta\bin\java.exe'
$g = 'C:\Users\Administrator\.gradle\caches\modules-2\files-2.1'
$gdx = (Get-ChildItem $g -Recurse -Filter 'gdx-1.13.5.jar' | Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1).FullName
$root = Join-Path $PSScriptRoot '..\runtime\core\src\main\java'
$cls = Join-Path $env:TEMP 'legacy-probe-cls'
Remove-Item $cls -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $cls | Out-Null
$src = Get-ChildItem (Join-Path $root 'pokemon\runtime\legacy') -Filter *.java | ForEach-Object { $_.FullName }
& $java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -nowarn -sourcepath $root -cp $gdx -d $cls $src
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
if (-not $Data) { $Data = Join-Path $PSScriptRoot '..\generated' }
$abs = ($Saves -split ';' | ForEach-Object { (Resolve-Path $_).Path }) -join ';'
& $java '-Dfile.encoding=UTF-8' -cp "$cls;$gdx" pokemon.runtime.legacy.LegacyProbe (Resolve-Path $Data).Path $abs $Out $Boxes
