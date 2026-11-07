param(
    [string]$StudyDirectory = 'build\startup-study\2026-11-07-swiftshader',
    [string]$StartupApk = 'app\build\outputs\apk\benchmark\app-benchmark.apk',
    [string]$OutputDirectory = ('build\startup-study\profiles-' + (Get-Date -Format 'yyyyMMdd-HHmmss')),
    [ValidateRange(3,20)][int]$Iterations = 5,
    [string]$Serial,
    [switch]$Emulator,
    [switch]$ProfilePairOnly,
    [switch]$ProfilesOnly,
    [ValidateSet('EFFE','FEEF')][string]$PairOrder='EFFE',
    [switch]$ExternalCompilation,
    [switch]$ConfirmBenchmarkInstallDialogs
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if ($ProfilePairOnly -and $ProfilesOnly) { throw 'Choose only one comparison mode.' }
    $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if ($Serial) { $env:ANDROID_SERIAL = $Serial }
    $devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' })
    if (-not $env:ANDROID_SERIAL) {
        if ($devices.Count -ne 1) { throw 'Connect one device or pass -Serial.' }
        $env:ANDROID_SERIAL = ($devices[0] -split '\s+')[0]
    }
    if ((& $adb shell getprop sys.boot_completed).Trim() -ne '1') { throw 'Android is not ready.' }
    $isEmulator = ((& $adb shell getprop ro.kernel.qemu).Trim() -eq '1')
    if ($isEmulator -and -not $Emulator) { throw 'Pass -Emulator for virtual-device measurements.' }
    if ((Test-Path $OutputDirectory) -and @(Get-ChildItem $OutputDirectory -Force).Count) { throw 'Choose an empty output directory.' }
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    & $adb shell dumpsys battery | Set-Content (Join-Path $OutputDirectory 'battery-before.txt') -Encoding UTF8
    & $adb shell dumpsys thermalservice | Set-Content (Join-Path $OutputDirectory 'thermal-before.txt') -Encoding UTF8
    $apkDirectory = Join-Path $OutputDirectory 'apks'
    New-Item -ItemType Directory -Force $apkDirectory | Out-Null
    $apkInfo = @()
    foreach ($entry in @(
        @{ id='A'; file=(Join-Path $StudyDirectory 'A-eager-unoptimized\app-benchmark.apk') },
        @{ id='E'; file=(Join-Path $StudyDirectory 'E-baseline-aot\app-benchmark.apk') },
        @{ id='F'; file=$StartupApk }
    )) {
        if ($entry.id -eq 'A' -and ($ProfilePairOnly -or $ProfilesOnly)) { continue }
        $copy = Join-Path $apkDirectory "$($entry.id).apk"
        Copy-Item -LiteralPath $entry.file -Destination $copy
        $apkInfo += [pscustomobject]@{ configuration=$entry.id; bytes=(Get-Item $copy).Length; sha256=(Get-FileHash $copy -Algorithm SHA256).Hash }
    }
    $apkInfo | ConvertTo-Json | Set-Content (Join-Path $OutputDirectory 'apk-info.json') -Encoding UTF8
    $sequence = if($ProfilesOnly){@('D1','E1','F1','F2','D2','E2','E3','F3','D3')}elseif($ProfilePairOnly){if($PairOrder -eq 'FEEF'){@('F1','E1','E2','F2')}else{@('E1','F1','F2','E2')}}else{@('A1','D1','E1','F1','F2','E2','D2','A2')}
    @{
        date=[DateTimeOffset]::Now.ToString('o'); serial=$env:ANDROID_SERIAL; emulator=$isEmulator
        fingerprint=(& $adb shell getprop ro.build.fingerprint).Trim()
        order=$sequence; iterationsPerBlock=$Iterations; scenario='Logged out cold start to login'
        D='Same APK as E; CompilationMode.None'; E='Lazy + R8 + Baseline; Partial Require'
        F='Lazy + R8 + Baseline + Startup; Partial Require'; A='Eager, no R8; None'
        noBuildBetweenBlocks=$true
        externalCompilation=[bool]$ExternalCompilation
        compilationPreparation=if($ExternalCompilation){'Fresh uninstall/install per block; skip automatic ProfileInstaller; D verify, E/F explicit INSTALL_PROFILE + speed-profile; benchmark compilation.enabled=false'}else{'Macrobenchmark CompilationMode handles reset/compile'}
    } | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $OutputDirectory 'methodology.json') -Encoding UTF8
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' }
    & .\gradlew.bat --stop *> (Join-Path $OutputDirectory 'stop-gradle.log')
    if ($LASTEXITCODE -ne 0) { throw 'Cannot stop Gradle.' }
    function Get-InstalledApkHash([string]$PackageName) {
        $paths=@(& $adb shell pm path $PackageName)
        if($LASTEXITCODE -ne 0 -or $paths.Count -ne 1 -or $paths[0] -notmatch '^package:(/[A-Za-z0-9_./~+=-]+)$'){return $null}
        $apkPath=$Matches[1]
        $hashOutput=(& $adb shell sha256sum $apkPath) -join ''
        if($LASTEXITCODE -eq 0 -and $hashOutput -match '^([0-9a-fA-F]{64})\s'){return $Matches[1].ToUpperInvariant()}
        return $null
    }
    function Install-BenchmarkTarget([string]$ApkPath,[string]$Folder) {
        if(-not $ConfirmBenchmarkInstallDialogs){
            & $adb install -r $ApkPath
            if($LASTEXITCODE -ne 0){throw 'Cannot install target.'}
            return
        }
        # Only confirm this locally built benchmark app. No general security settings change.
        $outFile=Join-Path ([IO.Path]::GetFullPath($Folder)) 'install.out.txt'
        $errFile=Join-Path ([IO.Path]::GetFullPath($Folder)) 'install.err.txt'
        $quotedApk='"'+[IO.Path]::GetFullPath($ApkPath)+'"'
        $installer=Start-Process -FilePath $adb -ArgumentList @('-s',$env:ANDROID_SERIAL,'install','-r',$quotedApk) -PassThru -WindowStyle Hidden -RedirectStandardOutput $outFile -RedirectStandardError $errFile
        $deadline=(Get-Date).AddMinutes(2)
        while(-not $installer.HasExited -and (Get-Date) -lt $deadline){
            $uiPreference=$ErrorActionPreference
            try {
                $ErrorActionPreference='Continue'
                & $adb shell uiautomator dump --compressed /data/local/tmp/food-benchmark-install.xml 2>$null | Out-Null
            } finally { $ErrorActionPreference=$uiPreference }
            $xmlText=(& $adb shell cat /data/local/tmp/food-benchmark-install.xml) -join ''
            try{[xml]$installUi=$xmlText}catch{continue}
            if(-not $installUi.SelectSingleNode('//node[@text="Food App Benchmark"]')){continue}
            $button=$installUi.SelectSingleNode('//node[@package="com.miui.securitycenter" and @text="Cài đặt" and @enabled="true"]')
            if(-not $button){$button=$installUi.SelectSingleNode('//node[@package="com.android.vending" and @text="Không gửi" and @enabled="true"]')}
            if($button -and $button.bounds -match '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$'){
                $tapX=[int]((([int]$Matches[1])+([int]$Matches[3]))/2)
                $tapY=[int]((([int]$Matches[2])+([int]$Matches[4]))/2)
                & $adb shell input tap $tapX $tapY | Out-Null
            }
            $installer.Refresh()
        }
        if(-not $installer.HasExited){throw 'Phone installation is still awaiting confirmation.'}
        $installer.WaitForExit()
        $installOutput=(Get-Content $outFile,$errFile -Raw) -join ''
        $installedHash=Get-InstalledApkHash 'com.example.jetpackcompose.benchmark'
        $expectedHash=(Get-FileHash $ApkPath -Algorithm SHA256).Hash
        if($installOutput -notmatch 'Success' -or $installedHash -ne $expectedHash){throw "Cannot verify installed benchmark target: $installOutput"}
    }
    $runner='macrobenchmark/build/outputs/apk/benchmark/macrobenchmark-benchmark.apk'
    $runnerHash=(Get-FileHash $runner -Algorithm SHA256).Hash
    if((Get-InstalledApkHash 'com.example.foodapp.macrobenchmark') -ne $runnerHash){
        & $adb install -r -t $runner
        if ($LASTEXITCODE -ne 0) { throw 'Cannot install test runner.' }
    }else{Write-Host 'Installed runner SHA-256 matches; reusing exact APK.'}
    & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    & $adb shell wm dismiss-keyguard | Out-Null
    $media = '/sdcard/Android/media/com.example.foodapp.macrobenchmark'
    $reportName = 'com.example.foodapp.macrobenchmark-benchmarkData.json'
    $rows=@()
    foreach ($block in $sequence) {
        $config=$block.Substring(0,1)
        $apkId=if($config -eq 'D'){'E'}else{$config}
        $method=if($config -in @('A','D')){'coldStartupNoCompilation'}else{'coldStartupWithBaselineProfile'}
        $folder=Join-Path $OutputDirectory $block
        New-Item -ItemType Directory -Force $folder | Out-Null
        Write-Host "Measuring $block ($Iterations cold starts) ..."
        if($ExternalCompilation -and (Get-InstalledApkHash 'com.example.jetpackcompose.benchmark')){
            & $adb uninstall com.example.jetpackcompose.benchmark
            if($LASTEXITCODE -ne 0){throw 'Cannot remove dedicated benchmark app for a fresh compilation state.'}
        }
        $expectedHash=($apkInfo | Where-Object {$_.configuration -eq $apkId}).sha256
        if((Get-InstalledApkHash 'com.example.jetpackcompose.benchmark') -ne $expectedHash){
            Install-BenchmarkTarget (Join-Path $apkDirectory "$apkId.apk") $folder
        }else{Write-Host 'Installed target SHA-256 matches; resetting compilation without reinstall.'}
        if($ExternalCompilation){
            $preparation=@()
            $preparation+=& $adb shell am broadcast -a androidx.profileinstaller.action.SKIP_FILE -e EXTRA_SKIP_FILE_OPERATION WRITE_SKIP_FILE -n com.example.jetpackcompose.benchmark/androidx.profileinstaller.ProfileInstallReceiver
            & $adb shell am force-stop com.example.jetpackcompose.benchmark
            $verifyOutput=& $adb shell cmd package compile -f -m verify com.example.jetpackcompose.benchmark
            $preparation+=$verifyOutput
            if($LASTEXITCODE -ne 0 -or ($verifyOutput -join '') -notmatch 'Success'){throw 'Cannot prepare verify compilation.'}
            if($config -in @('E','F')){
                $profileOutput=& $adb shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n com.example.jetpackcompose.benchmark/androidx.profileinstaller.ProfileInstallReceiver
                $preparation+=$profileOutput
                if(($profileOutput -join '') -notmatch 'result=1\b'){throw 'Baseline Profile installation failed.'}
                & $adb shell am force-stop com.example.jetpackcompose.benchmark
                $compileOutput=& $adb shell cmd package compile -f -m speed-profile com.example.jetpackcompose.benchmark
                $preparation+=$compileOutput
                if($LASTEXITCODE -ne 0 -or ($compileOutput -join '') -notmatch 'Success'){throw 'Baseline compilation failed.'}
            }
            & $adb shell am force-stop com.example.jetpackcompose.benchmark
            $preparation | Set-Content (Join-Path $folder 'external-compilation.log') -Encoding UTF8
            & $adb shell dumpsys package com.example.jetpackcompose.benchmark | Select-String -Pattern 'Dexopt state|status=|compiler-filter|reason=' -Context 0,2 | Set-Content (Join-Path $folder 'dexopt-before.txt') -Encoding UTF8
        }
        & $adb shell rm -f "$media/$reportName"
        $argsExtra=@()
        if($isEmulator){$argsExtra=@('-e','androidx.benchmark.suppressErrors','EMULATOR')}
        if($ExternalCompilation){$argsExtra+=@('-e','androidx.benchmark.compilation.enabled','false')}
        $previous=$ErrorActionPreference
        try{
            $ErrorActionPreference='Continue'
            $log=& $adb shell am instrument -w -r -e class "com.example.foodapp.macrobenchmark.StartupBenchmark#$method" `
                -e iterations $Iterations @argsExtra -e androidx.benchmark.enabledRules Macrobenchmark `
                com.example.foodapp.macrobenchmark/androidx.test.runner.AndroidJUnitRunner 2>&1
            $nativeExit=$LASTEXITCODE
        }finally{$ErrorActionPreference=$previous}
        $log | Set-Content (Join-Path $folder 'instrumentation.log') -Encoding UTF8
        if($nativeExit -ne 0 -or ($log -match 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=') -or -not ($log -match 'OK \(1 test\)')){throw "Test failed: $block"}
        & $adb shell dumpsys package com.example.jetpackcompose.benchmark | Select-String -Pattern 'Dexopt state|status=|compiler-filter|reason=' -Context 0,2 | Set-Content (Join-Path $folder 'dexopt-state.txt') -Encoding UTF8
        & $adb shell dumpsys battery | Set-Content (Join-Path $folder 'battery.txt') -Encoding UTF8
        & $adb pull "$media/$reportName" (Join-Path $folder 'benchmarkData.json')
        if($LASTEXITCODE -ne 0){throw 'Cannot retrieve fresh results.'}
        $data=Get-Content (Join-Path $folder 'benchmarkData.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        if(@($data.benchmarks).Count -ne 1 -or $data.benchmarks[0].name -ne $method){throw 'Unexpected test identity.'}
        foreach($metric in @('timeToInitialDisplayMs','timeToFullDisplayMs')){
            $runs=@($data.benchmarks[0].metrics.$metric.runs)
            if($runs.Count -ne $Iterations){throw 'Wrong number of runs.'}
            for($i=0;$i -lt $runs.Count;$i++){
                $rows += [pscustomobject]@{configuration=$config;block=$block;iteration=$i+1;metric=$metric;milliseconds=[double]$runs[$i]}
            }
        }
        foreach($trace in $data.benchmarks[0].profilerOutputs){
            & $adb pull "$media/$($trace.filename)" (Join-Path $folder $trace.filename) | Out-Null
            if($LASTEXITCODE -ne 0){throw 'Cannot retrieve trace.'}
        }
        $rows | Export-Csv (Join-Path $OutputDirectory 'raw-runs.csv') -NoTypeInformation -Encoding UTF8
        Write-Host "Completed $block."
    }
    $measuredConfigurations=if($ProfilesOnly){@('D','E','F')}elseif($ProfilePairOnly){@('E','F')}else{@('A','D','E','F')}
    $statistics=foreach($config in $measuredConfigurations){
        foreach($metric in @('timeToInitialDisplayMs','timeToFullDisplayMs')){
            $values=@($rows | Where-Object {$_.configuration -eq $config -and $_.metric -eq $metric} | ForEach-Object {$_.milliseconds} | Sort-Object)
            if ($values.Count -eq 0) { throw 'No observations for requested configuration.' }
            $middle=[int][Math]::Floor($values.Count/2)
            $median=if($values.Count % 2){$values[$middle]}else{($values[$middle-1]+$values[$middle])/2}
            [pscustomobject]@{configuration=$config;metric=$metric;n=$values.Count;medianMs=$median;minimumMs=$values[0];maximumMs=$values[-1];meanMs=($values|Measure-Object -Average).Average}
        }
    }
    $statistics | Export-Csv (Join-Path $OutputDirectory 'statistics.csv') -NoTypeInformation -Encoding UTF8
    $statistics | ConvertTo-Json | Set-Content (Join-Path $OutputDirectory 'statistics.json') -Encoding UTF8
    $statistics | Format-Table -AutoSize
    & $adb shell dumpsys thermalservice | Set-Content (Join-Path $OutputDirectory 'thermal-after.txt') -Encoding UTF8
    Write-Host "Verified profile comparison: $OutputDirectory"
}finally{Pop-Location}
