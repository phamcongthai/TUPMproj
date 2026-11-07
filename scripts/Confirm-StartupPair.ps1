param(
    [string]$StudyDirectory = 'build\startup-study\2026-11-07-swiftshader',
    [string]$OutputDirectory = ('build\startup-study\pair-' + (Get-Date -Format 'yyyyMMdd-HHmmss')),
    [ValidateRange(3, 20)][int]$Iterations = 5,
    [string]$Serial,
    [switch]$Emulator
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
    if ($Serial) { $env:ANDROID_SERIAL = $Serial }
    $devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' })
    if (-not $env:ANDROID_SERIAL) {
        if ($devices.Count -ne 1) { throw 'Connect exactly one device or pass -Serial.' }
        $env:ANDROID_SERIAL = ($devices[0] -split '\s+')[0]
    }
    if ((& $adb shell getprop sys.boot_completed).Trim() -ne '1') { throw 'Wait for Android to finish booting.' }
    $virtualDevice = ((& $adb shell getprop ro.kernel.qemu).Trim() -eq '1')
    if ($virtualDevice -and -not $Emulator) { throw 'Explicitly pass -Emulator for emulator measurements.' }
    if ((Test-Path $OutputDirectory) -and @(Get-ChildItem $OutputDirectory -Force).Count -gt 0) {
        throw 'Output directory is not empty. Choose a new directory to preserve existing evidence.'
    }
    foreach ($name in @('A-eager-unoptimized','E-baseline-aot')) {
        $apk = Join-Path $StudyDirectory "$name\app-benchmark.apk"
        $info = Get-Content (Join-Path $StudyDirectory "$name\apk-info.json") -Raw -Encoding UTF8 | ConvertFrom-Json
        if ((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash -ne $info.sha256) { throw "APK hash mismatch: $name" }
    }
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    @{
        date=[DateTimeOffset]::Now.ToString('o'); serial=$env:ANDROID_SERIAL; emulator=$virtualDevice
        fingerprint=(& $adb shell getprop ro.build.fingerprint).Trim()
        confirmationOrder=@('A1','E1','E2','A2'); iterationsPerBlock=$Iterations
        scenario='Logged out, login screen ready'; studyDirectory=[IO.Path]::GetFullPath($StudyDirectory)
    } | ConvertTo-Json | Set-Content (Join-Path $OutputDirectory 'methodology.json') -Encoding UTF8
    & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    & $adb shell wm dismiss-keyguard | Out-Null
    # Build separately first. Do not retain the R8/Gradle build daemon during this confirmation.
    & .\gradlew.bat --stop *> (Join-Path $OutputDirectory 'stop-gradle.log')
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop Gradle.' }
    & $adb install -r -t macrobenchmark/build/outputs/apk/benchmark/macrobenchmark-benchmark.apk
    if ($LASTEXITCODE -ne 0) { throw 'Could not install test APK.' }
    $sequence = @(
        @{ name='A1'; config='A-eager-unoptimized'; method='coldStartupNoCompilation' },
        @{ name='E1'; config='E-baseline-aot'; method='coldStartupWithBaselineProfile' },
        @{ name='E2'; config='E-baseline-aot'; method='coldStartupWithBaselineProfile' },
        @{ name='A2'; config='A-eager-unoptimized'; method='coldStartupNoCompilation' }
    )
    foreach ($phase in $sequence) {
        $phaseDirectory = Join-Path $OutputDirectory $phase.name
        New-Item -ItemType Directory -Force $phaseDirectory | Out-Null
        Write-Host "Confirming $($phase.name) ..."
        & $adb install -r (Join-Path $StudyDirectory "$($phase.config)\app-benchmark.apk")
        if ($LASTEXITCODE -ne 0) { throw 'Could not install target APK.' }
        $previousPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $emulatorArguments = @()
            if ($virtualDevice) { $emulatorArguments = @('-e', 'androidx.benchmark.suppressErrors', 'EMULATOR') }
            $instrumentationOutput = & $adb shell am instrument -w -r `
                -e class "com.example.foodapp.macrobenchmark.StartupBenchmark#$($phase.method)" `
                -e iterations $Iterations `
                @emulatorArguments `
                -e androidx.benchmark.enabledRules Macrobenchmark `
                com.example.foodapp.macrobenchmark/androidx.test.runner.AndroidJUnitRunner 2>&1
            $instrumentationExit = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousPreference }
        $instrumentationOutput | Set-Content (Join-Path $phaseDirectory 'instrumentation.log') -Encoding UTF8
        if ($instrumentationExit -ne 0 -or ($instrumentationOutput -match 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=')) {
            throw "Instrumentation failed in $($phase.name)."
        }
        & $adb pull /sdcard/Android/media/com.example.foodapp.macrobenchmark/com.example.foodapp.macrobenchmark-benchmarkData.json (Join-Path $phaseDirectory 'benchmarkData.json')
        if ($LASTEXITCODE -ne 0) { throw 'Could not retrieve benchmark JSON.' }
        $phaseData = Get-Content (Join-Path $phaseDirectory 'benchmarkData.json') -Raw | ConvertFrom-Json
        if ($phaseData.benchmarks.Count -ne 1 -or $phaseData.benchmarks[0].name -ne $phase.method -or
            $phaseData.benchmarks[0].metrics.timeToInitialDisplayMs.runs.Count -ne $Iterations) {
            throw 'Unexpected benchmark identity or iteration count.'
        }
        & $adb pull /sdcard/Android/media/com.example.foodapp.macrobenchmark (Join-Path $phaseDirectory 'device-output')
        if ($LASTEXITCODE -ne 0) { throw 'Could not retrieve traces.' }
    }
    & (Join-Path $PSScriptRoot 'Export-StartupResults.ps1') -PairDirectory $OutputDirectory `
        -StudyDirectory $StudyDirectory -OutputDirectory (Join-Path $OutputDirectory 'analysis') -IterationsPerBlock $Iterations
    Write-Host "Counterbalanced raw results: $OutputDirectory"
} finally { Pop-Location }
