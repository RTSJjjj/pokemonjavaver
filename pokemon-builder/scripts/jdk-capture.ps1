# Gradle-free MenuCapture run (for environments where Gradle fails with "Unable to establish loopback connection").
# Usage: powershell -File scripts\jdk-capture.ps1 <dataRoot> <outDir> <mode>   (modes: see MenuCapture, e.g. double, double-trainer, double-switch, double-faint)
param([string]$DataRoot, [string]$OutDir, [string]$Mode = '')
$ErrorActionPreference = 'Stop'
$java = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta\bin\java.exe'
$g = 'C:\Users\Administrator\.gradle\caches\modules-2\files-2.1'
function Jar($pat) { (Get-ChildItem $g -Recurse -Filter $pat | Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1).FullName }
$names = @('gdx-1.13.5.jar', 'gdx-freetype-1.13.5.jar', 'gdx-jnigen-loader-2.5.2.jar', 'gdx-backend-lwjgl3-1.13.5.jar',
    'gdx-platform-1.13.5-natives-desktop.jar', 'gdx-freetype-platform-1.13.5-natives-desktop.jar')
foreach ($m in 'lwjgl', 'lwjgl-glfw', 'lwjgl-opengl', 'lwjgl-openal', 'lwjgl-stb', 'lwjgl-jemalloc', 'lwjgl-freetype', 'lwjgl-tinyfd') {
    $names += "$m-3.3.3.jar"
    $names += "$m-3.3.3-natives-windows.jar"
}
$libs = $names | ForEach-Object { $j = Jar $_; if (-not $j) { Write-Warning "missing $_" }; $j } | Where-Object { $_ }
$cp = ($libs -join ';')
$root = Join-Path $PSScriptRoot '..'
$out = Join-Path $env:TEMP 'pokemon-capture-jdk'
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
$mainOut = "$out\core"; $lwOut = "$out\lwjgl3"
New-Item -ItemType Directory -Force $mainOut, $lwOut | Out-Null
function Compile($srcDir, $dest, $classpath) {
    $list = "$dest.txt"
    Push-Location $srcDir
    try {
        Get-ChildItem . -Recurse -Filter *.java | ForEach-Object { '"' + ((Resolve-Path -Relative $_.FullName) -replace '\\', '/') + '"' } | Set-Content $list -Encoding ASCII
        & $java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -nowarn -d $dest -cp $classpath "@$list"
        if ($LASTEXITCODE -ne 0) { throw "javac failed for $srcDir" }
    } finally { Pop-Location }
}
Compile "$root\runtime\core\src\main\java" $mainOut $cp
Compile "$root\runtime\lwjgl3\src\main\java" $lwOut ($cp + ';' + $mainOut)
# The capture tools are a separate source set (runtime/lwjgl3/src/devtools) and are not packed into the game.
$devOut = "$out\devtools"
New-Item -ItemType Directory -Force $devOut | Out-Null
Compile "$root\runtime\lwjgl3\src\devtools\java" $devOut ($cp + ';' + $mainOut + ';' + $lwOut)
$res = "$root\runtime\core\src\main\resources"
$full = $cp + ';' + $mainOut + ';' + $lwOut + ';' + $devOut
if (Test-Path $res) { $full += ';' + $res }
$mainClass = if ($env:POKEMON_CAPTURE_MAIN) { $env:POKEMON_CAPTURE_MAIN } else { 'pokemon.runtime.lwjgl3.MenuCapture' }
& $java '-Dfile.encoding=UTF-8' -cp $full $mainClass $DataRoot $OutDir $Mode
exit $LASTEXITCODE
