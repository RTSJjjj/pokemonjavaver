# Pokemon Builder - visual build generator (Windows PowerShell 5.1, WinForms).
# Pick the game project in Explorer, then export the PC or the Android package. Everything runs through builder.bat.
param([string]$Project = '')

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$builderBat = Join-Path $root 'builder.bat'
$settingsDir = Join-Path $env:LOCALAPPDATA 'PokemonBuilder'
$settingsFile = Join-Path $settingsDir 'gui.json'
$script:process = $null
$script:logFile = Join-Path ([System.IO.Path]::GetTempPath()) ('pokemon-builder-gui-' + $PID + '.log')
$script:logPosition = 0L
$script:currentLabel = ''
$script:manual = @{ node = ''; java = ''; ffmpeg = ''; sdk = '' }

function Load-Settings {
    $s = @{ project = ''; encrypt = $true; release = $false; noCache = $false }
    try {
        if (Test-Path $settingsFile) {
            $json = Get-Content -Raw -Encoding UTF8 $settingsFile | ConvertFrom-Json
            if ($json.project) { $s.project = [string]$json.project }
            if ($null -ne $json.encrypt) { $s.encrypt = [bool]$json.encrypt }
            if ($null -ne $json.release) { $s.release = [bool]$json.release }
            if ($null -ne $json.noCache) { $s.noCache = [bool]$json.noCache }
            foreach ($key in 'node', 'java', 'ffmpeg', 'sdk') {
                $value = $json.manual.$key
                if ($value) { $script:manual[$key] = [string]$value }
            }
        }
    } catch { }
    return $s
}

function Save-Settings {
    try {
        New-Item -ItemType Directory -Force $settingsDir | Out-Null
        $obj = [ordered]@{
            project = $txtProject.Text
            encrypt = $chkEncrypt.Checked
            release = $chkRelease.Checked
            noCache = $chkNoCache.Checked
            manual = $script:manual
        }
        ($obj | ConvertTo-Json) | Set-Content -Encoding UTF8 $settingsFile
    } catch { }
}

function Test-Project([string]$path) {
    if ([string]::IsNullOrWhiteSpace($path) -or -not (Test-Path -LiteralPath $path -PathType Container)) { return $false }
    return (Test-Path -LiteralPath (Join-Path $path 'Data\System.rxdata'))
}

function Test-Sdk([string]$path) {
    if ([string]::IsNullOrWhiteSpace($path) -or -not (Test-Path -LiteralPath $path -PathType Container)) { return $false }
    foreach ($sub in 'platforms', 'build-tools', 'platform-tools') {
        if (Test-Path -LiteralPath (Join-Path $path $sub)) { return $true }
    }
    return $false
}

function Get-JavaStatus {
    if ($script:manual.java -and (Test-Path (Join-Path $script:manual.java 'bin\java.exe'))) { return "已找到（手动定位）" }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { return "已找到（JAVA_HOME）" }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) { return "已找到（PATH）" }
    return $null
}

function Get-AndroidSdkPath {
    if (Test-Sdk $script:manual.sdk) { return $script:manual.sdk }
    foreach ($name in 'ANDROID_HOME', 'ANDROID_SDK_ROOT') {
        $value = [Environment]::GetEnvironmentVariable($name)
        if ($value -and (Test-Path $value)) { return $value }
        # a variable set after this window's parent process started is still in the user's environment
        $value = [Environment]::GetEnvironmentVariable($name, 'User')
        if ($value -and (Test-Path $value)) { return $value }
    }
    $props = Join-Path $root 'runtime\local.properties'
    if (Test-Path $props) {
        $line = Select-String -Path $props -Pattern '^sdk\.dir=(.+)$' | Select-Object -First 1
        if ($line) {
            $dir = $line.Matches[0].Groups[1].Value.Replace('\\', '\').Replace('\:', ':')
            if (Test-Path $dir) { return $dir }
        }
    }
    foreach ($candidate in (Join-Path $env:LOCALAPPDATA 'Android\Sdk'), 'C:\Android\Sdk', 'D:\Android\Sdk', 'E:\Android\Sdk', (Join-Path $env:USERPROFILE 'Android\Sdk')) {
        if (Test-Path (Join-Path $candidate 'platforms')) { return $candidate }
    }
    return $null
}

function Get-AndroidSdkStatus {
    if (Get-AndroidSdkPath) { return '已找到' }
    return $null
}

function Append-Log([string]$text, [System.Drawing.Color]$color) {
    if ([string]::IsNullOrEmpty($text)) { return }
    $logBox.SelectionStart = $logBox.TextLength
    $logBox.SelectionLength = 0
    $logBox.SelectionColor = $color
    $logBox.AppendText($text)
    $logBox.SelectionColor = $logBox.ForeColor
    $logBox.ScrollToCaret()
}

function Update-Environment {
    $lines = @()
    $okColor = [System.Drawing.Color]::FromArgb(40, 150, 70)
    $badColor = [System.Drawing.Color]::FromArgb(200, 60, 50)
    $items = @(
        @{ name = 'Node.js'; value = $(if ($script:manual.node -and (Test-Path $script:manual.node)) { '已找到（手动定位）' } elseif (Get-Command node -ErrorAction SilentlyContinue) { '已找到' } else { $null }); lbl = $lblNode },
        @{ name = 'Java (JDK)'; value = (Get-JavaStatus); lbl = $lblJava },
        @{ name = 'ffmpeg'; value = $(if (Test-Path (Join-Path $root 'tools\ffmpeg\bin\ffmpeg.exe')) { '已内置' } elseif ($script:manual.ffmpeg -and (Test-Path $script:manual.ffmpeg)) { '已找到（手动定位）' } elseif (Get-Command ffmpeg -ErrorAction SilentlyContinue) { '已找到（PATH）' } else { $null }); lbl = $lblFfmpeg },
        @{ name = 'Android SDK'; value = (Get-AndroidSdkStatus); lbl = $lblSdk }
    )
    foreach ($item in $items) {
        if ($item.value) {
            $item.lbl.Text = $item.name + '：' + $item.value
            $item.lbl.ForeColor = $okColor
        } else {
            $suffix = if ($item.name -eq 'Android SDK') { '未找到（仅安卓需要）' } else { '未找到' }
            $item.lbl.Text = $item.name + '：' + $suffix
            $item.lbl.ForeColor = $badColor
        }
    }
}

# Pick a tool by hand when it is not found: Explorer opens, the choice is checked, remembered and handed to the builds.
function Locate-Tool([string]$kind) {
    $picked = $null
    switch ($kind) {
        'sdk' {
            $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
            $dialog.Description = '选择 Android SDK 文件夹（里面有 platforms、build-tools、platform-tools）'
            $dialog.ShowNewFolderButton = $false
            if ($dialog.ShowDialog($form) -eq 'OK') {
                if (Test-Sdk $dialog.SelectedPath) { $picked = $dialog.SelectedPath }
                else { [System.Windows.Forms.MessageBox]::Show('这个文件夹里没有 platforms / build-tools / platform-tools，不像 Android SDK。', '手动定位', 'OK', 'Warning') | Out-Null }
            }
        }
        'java' {
            $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
            $dialog.Description = '选择 JDK 文件夹（里面有 bin\java.exe）'
            $dialog.ShowNewFolderButton = $false
            if ($dialog.ShowDialog($form) -eq 'OK') {
                if (Test-Path (Join-Path $dialog.SelectedPath 'bin\java.exe')) { $picked = $dialog.SelectedPath }
                else { [System.Windows.Forms.MessageBox]::Show('这个文件夹里没有 bin\java.exe。', '手动定位', 'OK', 'Warning') | Out-Null }
            }
        }
        default {
            $dialog = New-Object System.Windows.Forms.OpenFileDialog
            $dialog.Title = if ($kind -eq 'node') { '选择 node.exe' } else { '选择 ffmpeg.exe' }
            $dialog.Filter = if ($kind -eq 'node') { 'node.exe|node.exe|程序 (*.exe)|*.exe' } else { 'ffmpeg.exe|ffmpeg.exe|程序 (*.exe)|*.exe' }
            if ($dialog.ShowDialog($form) -eq 'OK') { $picked = $dialog.FileName }
        }
    }
    if ($picked) {
        $script:manual[$kind] = $picked
        Save-Settings
        Update-Environment
    }
}

function Update-ProjectStatus {
    if ([string]::IsNullOrWhiteSpace($txtProject.Text)) {
        $lblProjectState.Text = '请选择游戏工程文件夹（里面有 Data、Graphics、Audio 的那个文件夹）'
        $lblProjectState.ForeColor = [System.Drawing.Color]::DimGray
    } elseif (Test-Project $txtProject.Text) {
        $lblProjectState.Text = '已找到 RMXP 工程'
        $lblProjectState.ForeColor = [System.Drawing.Color]::FromArgb(40, 150, 70)
    } else {
        $lblProjectState.Text = '这不是 RMXP 工程文件夹（缺少 Data\System.rxdata）'
        $lblProjectState.ForeColor = [System.Drawing.Color]::FromArgb(200, 60, 50)
    }
    $ready = (Test-Project $txtProject.Text) -and ($null -eq $script:process)
    $btnPc.Enabled = $ready
    $btnAndroid.Enabled = $ready
}

function Set-Running([bool]$running) {
    $btnPc.Enabled = -not $running
    $btnAndroid.Enabled = -not $running
    $btnBrowse.Enabled = -not $running
    $txtProject.Enabled = -not $running
    $btnStop.Enabled = $running
    $btnClean.Enabled = -not $running
    $progress.Style = if ($running) { 'Marquee' } else { 'Blocks' }
    $progress.MarqueeAnimationSpeed = if ($running) { 30 } else { 0 }
    if (-not $running) { Update-ProjectStatus }
}

function Read-LogTail {
    if (-not (Test-Path $script:logFile)) { return }
    try {
        $stream = New-Object System.IO.FileStream($script:logFile, 'Open', 'Read', 'ReadWrite')
        try {
            if ($stream.Length -gt $script:logPosition) {
                [void]$stream.Seek($script:logPosition, 'Begin')
                $bytes = New-Object byte[] ($stream.Length - $script:logPosition)
                $read = $stream.Read($bytes, 0, $bytes.Length)
                $script:logPosition += $read
                $text = [System.Text.Encoding]::UTF8.GetString($bytes, 0, $read)
                $color = [System.Drawing.Color]::FromArgb(40, 40, 40)
                Append-Log $text $color
            }
        } finally { $stream.Dispose() }
    } catch { }
}

function Start-Build([string]$command, [string[]]$flags, [string]$label, [bool]$withProject) {
    if ($null -ne $script:process) { return }
    if (-not (Test-Path $builderBat)) {
        [System.Windows.Forms.MessageBox]::Show("找不到 builder.bat：$builderBat", '构建器', 'OK', 'Error') | Out-Null
        return
    }
    $script:currentLabel = $label
    $script:logPosition = 0L
    if (Test-Path $script:logFile) { Remove-Item -Force $script:logFile -ErrorAction SilentlyContinue }
    $logBox.Clear()
    Append-Log ("=== " + $label + "  " + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + " ===`r`n") ([System.Drawing.Color]::FromArgb(30, 90, 170))
    $argText = $command
    if ($withProject) { $argText += ' "' + $txtProject.Text.TrimEnd('\') + '"' }
    foreach ($flag in $flags) { $argText += ' ' + $flag }
    $cmd = 'chcp 65001 >nul & call "' + $builderBat + '" ' + $argText + ' > "' + $script:logFile + '" 2>&1'
    $sdkPath = Get-AndroidSdkPath                       # hand the SDK to the build even when this window started before it was set
    if (Test-Sdk $script:manual.sdk) {
        $env:ANDROID_HOME = $script:manual.sdk
        $env:ANDROID_SDK_ROOT = $script:manual.sdk
    } elseif ($sdkPath) {
        # always overwrite: a stale value inherited by this window would otherwise win and Gradle rejects it
        $env:ANDROID_HOME = $sdkPath
        $env:ANDROID_SDK_ROOT = $sdkPath
    }
    if ($script:manual.java -and (Test-Path (Join-Path $script:manual.java 'bin\java.exe'))) { $env:JAVA_HOME = $script:manual.java }
    foreach ($key in 'node', 'ffmpeg') {                 # the folder of a located program goes first on PATH
        $file = $script:manual[$key]
        if ($file -and (Test-Path $file)) { $env:PATH = (Split-Path -Parent $file) + ';' + $env:PATH }
    }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'cmd.exe'
    $psi.Arguments = '/d /c "' + $cmd + '"'
    $psi.WorkingDirectory = $root
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    try {
        $script:process = [System.Diagnostics.Process]::Start($psi)
    } catch {
        Append-Log ("无法启动构建：" + $_.Exception.Message + "`r`n") ([System.Drawing.Color]::Firebrick)
        $script:process = $null
        return
    }
    Save-Settings
    Set-Running $true
    $timer.Start()
}

function Finish-Build {
    $timer.Stop()
    Read-LogTail
    $code = $script:process.ExitCode
    $script:process = $null
    if ($code -eq 0) {
        Append-Log ("`r`n=== " + $script:currentLabel + " 完成 ===`r`n") ([System.Drawing.Color]::FromArgb(40, 150, 70))
        $lblResult.Text = $script:currentLabel + ' 完成'
        $lblResult.ForeColor = [System.Drawing.Color]::FromArgb(40, 150, 70)
    } else {
        Append-Log ("`r`n=== " + $script:currentLabel + " 失败（退出码 " + $code + "），请看上面的日志 ===`r`n") ([System.Drawing.Color]::Firebrick)
        $lblResult.Text = $script:currentLabel + ' 失败'
        $lblResult.ForeColor = [System.Drawing.Color]::Firebrick
    }
    Set-Running $false
}

# ------------------------------------------------------------------ the window
$settings = Load-Settings
if ($Project) { $settings.project = $Project }

$form = New-Object System.Windows.Forms.Form
$form.Text = 'Pokemon 构建器'
$form.StartPosition = 'CenterScreen'
$form.ClientSize = New-Object System.Drawing.Size(820, 660)
$form.MinimumSize = New-Object System.Drawing.Size(760, 600)
$form.Font = New-Object System.Drawing.Font('Microsoft YaHei UI', 9)
$form.AutoScaleMode = 'Dpi'

$lblTitle = New-Object System.Windows.Forms.Label
$lblTitle.Text = '游戏工程'
$lblTitle.Font = New-Object System.Drawing.Font('Microsoft YaHei UI', 11, [System.Drawing.FontStyle]::Bold)
$lblTitle.Location = New-Object System.Drawing.Point(16, 14)
$lblTitle.AutoSize = $true
$form.Controls.Add($lblTitle)

$txtProject = New-Object System.Windows.Forms.TextBox
$txtProject.Location = New-Object System.Drawing.Point(16, 44)
$txtProject.Size = New-Object System.Drawing.Size(680, 26)
$txtProject.Anchor = 'Top,Left,Right'
$txtProject.Text = $settings.project
$form.Controls.Add($txtProject)

$btnBrowse = New-Object System.Windows.Forms.Button
$btnBrowse.Text = '浏览…'
$btnBrowse.Location = New-Object System.Drawing.Point(706, 42)
$btnBrowse.Size = New-Object System.Drawing.Size(98, 30)
$btnBrowse.Anchor = 'Top,Right'
$form.Controls.Add($btnBrowse)

$lblProjectState = New-Object System.Windows.Forms.Label
$lblProjectState.Location = New-Object System.Drawing.Point(16, 76)
$lblProjectState.Size = New-Object System.Drawing.Size(780, 20)
$lblProjectState.Anchor = 'Top,Left,Right'
$form.Controls.Add($lblProjectState)

$grpExport = New-Object System.Windows.Forms.GroupBox
$grpExport.Text = '导出'
$grpExport.Location = New-Object System.Drawing.Point(16, 106)
$grpExport.Size = New-Object System.Drawing.Size(788, 140)
$grpExport.Anchor = 'Top,Left,Right'
$form.Controls.Add($grpExport)

$btnPc = New-Object System.Windows.Forms.Button
$btnPc.Text = '导出 PC 版（Windows）'
$btnPc.Font = New-Object System.Drawing.Font('Microsoft YaHei UI', 10, [System.Drawing.FontStyle]::Bold)
$btnPc.Location = New-Object System.Drawing.Point(16, 26)
$btnPc.Size = New-Object System.Drawing.Size(250, 48)
$grpExport.Controls.Add($btnPc)

$btnAndroid = New-Object System.Windows.Forms.Button
$btnAndroid.Text = '导出 安卓版（APK）'
$btnAndroid.Font = New-Object System.Drawing.Font('Microsoft YaHei UI', 10, [System.Drawing.FontStyle]::Bold)
$btnAndroid.Location = New-Object System.Drawing.Point(280, 26)
$btnAndroid.Size = New-Object System.Drawing.Size(250, 48)
$grpExport.Controls.Add($btnAndroid)

$btnStop = New-Object System.Windows.Forms.Button
$btnStop.Text = '停止'
$btnStop.Location = New-Object System.Drawing.Point(548, 26)
$btnStop.Size = New-Object System.Drawing.Size(100, 48)
$btnStop.Enabled = $false
$grpExport.Controls.Add($btnStop)

$btnOpenOut = New-Object System.Windows.Forms.Button
$btnOpenOut.Text = '打开输出文件夹'
$btnOpenOut.Location = New-Object System.Drawing.Point(658, 26)
$btnOpenOut.Size = New-Object System.Drawing.Size(116, 48)
$grpExport.Controls.Add($btnOpenOut)

$chkEncrypt = New-Object System.Windows.Forms.CheckBox
$chkEncrypt.Text = '加密资源文件（图片、数据、字体；音频不加密）'
$chkEncrypt.Location = New-Object System.Drawing.Point(18, 88)
$chkEncrypt.AutoSize = $true
$chkEncrypt.Checked = $settings.encrypt
$grpExport.Controls.Add($chkEncrypt)

$chkRelease = New-Object System.Windows.Forms.CheckBox
$chkRelease.Text = '安卓：正式签名包（--release）'
$chkRelease.Location = New-Object System.Drawing.Point(358, 88)
$chkRelease.AutoSize = $true
$chkRelease.Checked = $settings.release
$grpExport.Controls.Add($chkRelease)

$chkSlim = New-Object System.Windows.Forms.CheckBox
$chkSlim.Text = '安卓：不含游戏数据的瘦包（手机上已装过数据时用，包小、构建快）'
$chkSlim.Location = New-Object System.Drawing.Point(18, 110)
$chkSlim.AutoSize = $true
$grpExport.Controls.Add($chkSlim)

$chkNoCache = New-Object System.Windows.Forms.CheckBox
$chkNoCache.Text = '忽略缓存，全部重建'
$chkNoCache.Location = New-Object System.Drawing.Point(598, 88)
$chkNoCache.AutoSize = $true
$chkNoCache.Checked = $settings.noCache
$grpExport.Controls.Add($chkNoCache)

$grpEnv = New-Object System.Windows.Forms.GroupBox
$grpEnv.Text = '运行环境（不随包提供，需要你自己装好）'
$grpEnv.Location = New-Object System.Drawing.Point(16, 256)
$grpEnv.Size = New-Object System.Drawing.Size(788, 88)
$grpEnv.Anchor = 'Top,Left,Right'
$form.Controls.Add($grpEnv)

$lblNode = New-Object System.Windows.Forms.Label
$lblNode.Location = New-Object System.Drawing.Point(16, 24); $lblNode.Size = New-Object System.Drawing.Size(170, 20)
$grpEnv.Controls.Add($lblNode)
$lblJava = New-Object System.Windows.Forms.Label
$lblJava.Location = New-Object System.Drawing.Point(186, 24); $lblJava.Size = New-Object System.Drawing.Size(210, 20)
$grpEnv.Controls.Add($lblJava)
$lblFfmpeg = New-Object System.Windows.Forms.Label
$lblFfmpeg.Location = New-Object System.Drawing.Point(406, 24); $lblFfmpeg.Size = New-Object System.Drawing.Size(130, 20)
$grpEnv.Controls.Add($lblFfmpeg)
$lblSdk = New-Object System.Windows.Forms.Label
$lblSdk.Location = New-Object System.Drawing.Point(546, 24); $lblSdk.Size = New-Object System.Drawing.Size(236, 20)
$grpEnv.Controls.Add($lblSdk)

function New-LocateButton([int]$x, [string]$kind) {
    $b = New-Object System.Windows.Forms.Button
    $b.Text = '手动定位…'
    $b.Location = New-Object System.Drawing.Point($x, 52)
    $b.Size = New-Object System.Drawing.Size(100, 26)
    $b.Tag = $kind
    $b.Add_Click({
        param($sender, $e)
        Locate-Tool ([string]$sender.Tag)
    })
    $grpEnv.Controls.Add($b)
}
New-LocateButton 16 'node'
New-LocateButton 186 'java'
New-LocateButton 406 'ffmpeg'
New-LocateButton 546 'sdk'

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(16, 356)
$progress.Size = New-Object System.Drawing.Size(560, 12)
$progress.Anchor = 'Top,Left,Right'
$progress.Style = 'Blocks'
$form.Controls.Add($progress)

$lblResult = New-Object System.Windows.Forms.Label
$lblResult.Location = New-Object System.Drawing.Point(590, 352)
$lblResult.Size = New-Object System.Drawing.Size(214, 20)
$lblResult.Anchor = 'Top,Right'
$lblResult.TextAlign = 'MiddleRight'
$form.Controls.Add($lblResult)

$logBox = New-Object System.Windows.Forms.RichTextBox
$logBox.Location = New-Object System.Drawing.Point(16, 380)
$logBox.Size = New-Object System.Drawing.Size(788, 226)
$logBox.Anchor = 'Top,Bottom,Left,Right'
$logBox.ReadOnly = $true
$logBox.BackColor = [System.Drawing.Color]::FromArgb(250, 250, 250)
$logBox.Font = New-Object System.Drawing.Font('Consolas', 9)
$logBox.WordWrap = $false
$logBox.ScrollBars = 'Both'
$form.Controls.Add($logBox)

$btnClean = New-Object System.Windows.Forms.Button
$btnClean.Text = '清理构建缓存'
$btnClean.Location = New-Object System.Drawing.Point(16, 618)
$btnClean.Size = New-Object System.Drawing.Size(130, 30)
$btnClean.Anchor = 'Bottom,Left'
$form.Controls.Add($btnClean)

$btnOpenLogs = New-Object System.Windows.Forms.Button
$btnOpenLogs.Text = '打开日志文件夹'
$btnOpenLogs.Location = New-Object System.Drawing.Point(156, 618)
$btnOpenLogs.Size = New-Object System.Drawing.Size(130, 30)
$btnOpenLogs.Anchor = 'Bottom,Left'
$form.Controls.Add($btnOpenLogs)

$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 300

# ------------------------------------------------------------------ events
$btnBrowse.Add_Click({
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = '选择游戏工程文件夹（RPG Maker XP 工程：里面有 Data、Graphics、Audio）'
    $dialog.ShowNewFolderButton = $false
    if (Test-Path -LiteralPath $txtProject.Text -PathType Container) { $dialog.SelectedPath = $txtProject.Text }
    if ($dialog.ShowDialog($form) -eq 'OK') {
        $txtProject.Text = $dialog.SelectedPath
        Save-Settings
    }
})

$txtProject.Add_TextChanged({ Update-ProjectStatus })

$btnPc.Add_Click({
    $flags = @()
    if (-not $chkEncrypt.Checked) { $flags += '--no-encrypt' }
    if ($chkNoCache.Checked) { $flags += '--no-cache' }
    Start-Build 'build-pc' $flags '导出 PC 版' $true
})

$btnAndroid.Add_Click({
    $flags = @()
    if (-not $chkEncrypt.Checked) { $flags += '--no-encrypt' }
    if ($chkRelease.Checked) { $flags += '--release' }
    if ($chkSlim.Checked) { $flags += '--no-data' }
    if ($chkNoCache.Checked) { $flags += '--no-cache' }
    Start-Build 'build-android' $flags '导出 安卓版' $true
})

$btnClean.Add_Click({ Start-Build 'clean' @() '清理构建缓存' $false })

$btnStop.Add_Click({
    if ($null -ne $script:process) {
        try { & taskkill.exe /PID $script:process.Id /T /F | Out-Null } catch { }
    }
})

$btnOpenOut.Add_Click({
    $dist = Join-Path $root 'dist'
    New-Item -ItemType Directory -Force $dist | Out-Null
    Start-Process explorer.exe $dist
})

$btnOpenLogs.Add_Click({
    $logs = Join-Path $root 'logs'
    New-Item -ItemType Directory -Force $logs | Out-Null
    Start-Process explorer.exe $logs
})

$timer.Add_Tick({
    Read-LogTail
    if ($null -ne $script:process -and $script:process.HasExited) { Finish-Build }
})

$form.Add_FormClosing({
    param($sender, $e)
    if ($null -ne $script:process) {
        $answer = [System.Windows.Forms.MessageBox]::Show('构建还在进行，关闭窗口会中止它。确定关闭？', '构建器', 'YesNo', 'Warning')
        if ($answer -ne 'Yes') { $e.Cancel = $true; return }
        try { & taskkill.exe /PID $script:process.Id /T /F | Out-Null } catch { }
    }
    Save-Settings
    if (Test-Path $script:logFile) { Remove-Item -Force $script:logFile -ErrorAction SilentlyContinue }
})

Update-Environment
Update-ProjectStatus
[void]$form.ShowDialog()
