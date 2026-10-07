# Gradle-free build + test of runtime/core (for environments where Gradle fails
# with "Unable to establish loopback connection").
# Usage: powershell -File scripts\jdk-core-test.ps1 [-Filter <class-name-substring>] [-NoRun] [-ExcludeTests <test file names that do not compile yet>]
param([string]$Filter = '', [switch]$NoRun, [string[]]$ExcludeTests = @())
$ErrorActionPreference = 'Stop'
$java = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta\bin\java.exe'
$g = 'C:\Users\Administrator\.gradle\caches\modules-2\files-2.1'
function Jar($pat) { (Get-ChildItem $g -Recurse -Filter $pat | Where-Object { $_.Name -notmatch 'sources|javadoc|natives' } | Select-Object -First 1).FullName }
$libs = @('gdx-1.13.5.jar', 'gdx-freetype-1.13.5.jar', 'gdx-jnigen-loader-2.5.2.jar') | ForEach-Object { Jar $_ }
$testLibs = @('junit-jupiter-5.10.2.jar', 'junit-jupiter-api-5.10.2.jar', 'junit-jupiter-engine-5.10.2.jar', 'junit-jupiter-params-5.10.2.jar',
    'junit-platform-commons-1.10.2.jar', 'junit-platform-engine-1.10.2.jar', 'junit-platform-launcher-1.10.2.jar',
    'opentest4j-1.3.0.jar', 'apiguardian-api-1.1.2.jar') | ForEach-Object { Jar $_ }
$core = Join-Path $PSScriptRoot '..\runtime\core'
$out = Join-Path $env:TEMP 'pokemon-core-jdk'
$mainOut = "$out\main"; $testOut = "$out\test"
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $mainOut, $testOut | Out-Null
$cp = ($libs -join ';')
function Compile($srcDir, $dest, $classpath, $exclude = @()) {
    $list = "$out\" + (Split-Path $dest -Leaf) + '.txt'
    # Relative paths keep the argfile pure ASCII (the repo path has CJK characters).
    Push-Location $srcDir
    try {
        Get-ChildItem . -Recurse -Filter *.java | Where-Object { $exclude -notcontains $_.Name } | ForEach-Object { '"' + ((Resolve-Path -Relative $_.FullName) -replace '\\', '/') + '"' } | Set-Content $list -Encoding ASCII
        & $java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -nowarn -d $dest -cp $classpath "@$list"
        if ($LASTEXITCODE -ne 0) { throw "javac failed for $srcDir" }
    } finally { Pop-Location }
}
Compile "$core\src\main\java" $mainOut $cp
Compile "$core\src\test\java" $testOut ($cp + ';' + $mainOut + ';' + ($testLibs -join ';'))
if ($NoRun) { Write-Host 'COMPILE OK'; exit 0 }
$runner = "$out\Run.java"
@'
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
public class Run {
    public static void main(String[] a) throws Exception {
        var b = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClasspathRoots(java.util.Set.of(java.nio.file.Path.of(a[0]))));
        if (a.length > 1 && !a[1].isEmpty()) b.filters(includeClassNamePatterns(".*" + a[1] + ".*"));
        var l = new SummaryGeneratingListener();
        LauncherFactory.create().execute(b.build(), l);
        l.getSummary().printFailuresTo(new java.io.PrintWriter(System.out), 15);
        l.getSummary().printTo(new java.io.PrintWriter(System.out));
        System.exit(l.getSummary().getTotalFailureCount() == 0 ? 0 : 1);
    }
}
'@ | Set-Content $runner -Encoding ASCII
& $java -m jdk.compiler/com.sun.tools.javac.Main -d $out -cp ($testLibs -join ';') $runner
& $java '-Dfile.encoding=UTF-8' -cp ($cp + ';' + $mainOut + ';' + $testOut + ';' + ($testLibs -join ';') + ';' + $out) Run $testOut $Filter
exit $LASTEXITCODE
