param(
    [ValidateSet('Full', 'Generate', 'Measure')][string]$Mode = 'Full',
    [switch]$Emulator,
    [ValidateRange(3, 50)][int]$Iterations = 10,
    [string]$Serial,
    [switch]$Resume,
    [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    if (-not $env:JAVA_HOME) {
        $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
    }
    $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if ($Serial) { $env:ANDROID_SERIAL = $Serial }
    $devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' })
    if (-not $env:ANDROID_SERIAL) {
        if ($devices.Count -ne 1) { throw 'Connect exactly one unlocked device or pass -Serial.' }
        $env:ANDROID_SERIAL = ($devices[0] -split '\s+')[0]
    }
    if ((& $adb shell getprop sys.boot_completed).Trim() -ne '1') {
        throw 'Wait for Android to finish booting before running the study.'
    }
    $virtualDevice = ((& $adb shell getprop ro.kernel.qemu).Trim() -eq '1')
    if ($virtualDevice -and -not $Emulator) { throw 'For practice on an emulator, explicitly pass -Emulator.' }
    $apiLevel = [int]((& $adb shell getprop ro.build.version.sdk).Trim())
    if ($Mode -in @('Full', 'Generate') -and $apiLevel -lt 33) {
        throw 'Profile generation in this workflow requires Android 13/API 33 or newer (no root).'
    }
    & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    & $adb shell wm dismiss-keyguard | Out-Null

    if (-not $OutputDirectory) {
        $OutputDirectory = Join-Path $projectDirectory ('build\startup-study\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    }
    $OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    $common = @('--console=plain', '--max-workers=2')
    if ($Emulator) {
        $common += '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR'
    }
    $common += "-Pandroid.testInstrumentationRunnerArguments.iterations=$Iterations"
    $metadata = [ordered]@{
        date = [DateTimeOffset]::Now.ToString('o')
        serial = $env:ANDROID_SERIAL
        emulator = $virtualDevice
        model = (& $adb shell getprop ro.product.model).Trim()
        api = (& $adb shell getprop ro.build.version.sdk).Trim()
        iterations = $Iterations
        gitHead = (& git rev-parse HEAD)
        workingTree = @(& git status --short)
        scenario = 'Logged out; login screen; TTID and TTFD; no backend dependency'
    }
    if (-not ($Resume -and (Test-Path (Join-Path $OutputDirectory 'environment.json')))) {
        $metadata | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $OutputDirectory 'environment.json') -Encoding UTF8
        $sourceFiles = @(Get-ChildItem app/src, macrobenchmark/src, scripts, gradle -Recurse -File)
        $sourceFiles += Get-Item app/build.gradle.kts, app/proguard-rules.pro, macrobenchmark/build.gradle.kts, build.gradle.kts, settings.gradle.kts, gradle.properties
        $sourceFiles | ForEach-Object {
            [pscustomobject]@{
                path = $_.FullName.Substring($projectDirectory.Length + 1)
                sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
            }
        } | Export-Csv (Join-Path $OutputDirectory 'source-hashes.csv') -NoTypeInformation -Encoding UTF8
    }

    function Invoke-StudyGradle([string]$Name, [string[]]$Arguments) {
        Write-Host "Running $Name ..."
        $logFile = Join-Path $OutputDirectory "$Name.log"
        $previousPreference = $ErrorActionPreference
        try {
            # Windows PowerShell wraps native stderr warnings as error records.
            # Decide success using the process exit code, not presence of stderr.
            $ErrorActionPreference = 'Continue'
            & .\gradlew.bat @Arguments @common *> $logFile
            $gradleExitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousPreference }
        if ($gradleExitCode -ne 0) {
            Get-Content $logFile -Tail 40 | Write-Host
            throw "$Name failed. See $logFile"
        }
    }

    $additionalOutput = 'macrobenchmark\build\outputs\connected_android_test_additional_output'
    if ($Mode -in @('Full', 'Generate')) {
        Invoke-StudyGradle 'generate-profiles' @(
            ':macrobenchmark:connectedBenchmarkAndroidTest',
            '-PoptimizeBuild=false', '-PstartupOptimizations=true', '-PappProfiles=false',
            '-Pandroid.testInstrumentationRunnerArguments.class=com.example.foodapp.macrobenchmark.BaselineProfileGenerator',
            '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile'
        )
        foreach ($kind in @('baseline', 'startup')) {
            $generatedProfiles = @(Get-ChildItem $additionalOutput -Recurse -Filter "*-$kind-prof.txt")
            # BaselineProfileRule emits a startup HRF for includeInStartupProfile=true.
            # This study captures one startup journey only, so its rules belong to both profiles.
            if ($kind -eq 'baseline' -and $generatedProfiles.Count -eq 0) {
                $generatedProfiles = @(Get-ChildItem $additionalOutput -Recurse -Filter '*-startup-prof.txt')
            }
            if ($generatedProfiles.Count -ne 1) { throw "Expected one generated $kind profile, found $($generatedProfiles.Count)." }
            $destination = "app\src\main\profiles\$kind"
            New-Item -ItemType Directory -Force $destination | Out-Null
            Copy-Item -LiteralPath $generatedProfiles[0].FullName -Destination "$destination\$kind-prof.txt"
            Copy-Item -LiteralPath $generatedProfiles[0].FullName -Destination (Join-Path $OutputDirectory "$kind-prof.txt")
        }
    }
    if ($Mode -in @('Full', 'Generate')) {
        # Baseline covers both collected journeys, while Startup keeps initial-display rules only.
        # Merge flags from observed rules; do not mark unobserved methods as hot.
        $mergedRules = @{}
        foreach ($file in @('baseline', 'startup')) {
            foreach ($line in Get-Content "app/src/main/profiles/$file/$file-prof.txt") {
                if ($line -match '^([HSP]*)(L.+)$') {
                    $flags = $Matches[1]; $descriptor = $Matches[2]
                    $existing = [string]$mergedRules[$descriptor]
                    $mergedRules[$descriptor] = -join (@('H','S','P') | Where-Object { ($existing + $flags).Contains($_) })
                } elseif ($line.Trim() -and -not $line.StartsWith('#')) { throw 'Invalid captured profile rule.' }
            }
        }
        $mergedText = (@($mergedRules.Keys | Sort-Object | ForEach-Object { $mergedRules[$_] + $_ }) -join "`n") + "`n"
        [IO.File]::WriteAllText((Join-Path $projectDirectory 'app/src/main/profiles/baseline/baseline-prof.txt'), $mergedText, (New-Object Text.UTF8Encoding($false)))
        Copy-Item app/src/main/profiles/baseline/baseline-prof.txt (Join-Path $OutputDirectory 'baseline-merged-prof.txt')
    }
    if ($Mode -eq 'Generate') { Write-Host "Profiles generated. Logs: $OutputDirectory"; return }
    foreach ($kind in @('baseline', 'startup')) {
        if (-not (Test-Path "app\src\main\profiles\$kind\$kind-prof.txt")) {
            throw 'Missing profiles. Run -Mode Generate first.'
        }
    }

    # Identical login/ReportDrawn instrumentation in all configurations.
    $experiments = @(
        @{ name='A-eager-unoptimized'; lazy='false'; r8='false'; profiles='false'; startup='false'; method='coldStartupNoCompilation' },
        @{ name='B-lazy-unoptimized'; lazy='true'; r8='false'; profiles='false'; startup='false'; method='coldStartupNoCompilation' },
        @{ name='C-lazy-r8'; lazy='true'; r8='true'; profiles='false'; startup='false'; method='coldStartupNoCompilation' },
        @{ name='D-baseline-no-aot'; lazy='true'; r8='true'; profiles='true'; startup='false'; method='coldStartupNoCompilation' },
        @{ name='E-baseline-aot'; lazy='true'; r8='true'; profiles='true'; startup='false'; method='coldStartupWithBaselineProfile' },
        @{ name='F-baseline-startup-aot'; lazy='true'; r8='true'; profiles='true'; startup='true'; method='coldStartupWithBaselineProfile' },
        @{ name='G-baseline-without-r8'; lazy='true'; r8='false'; profiles='true'; startup='false'; method='coldStartupWithBaselineProfile' }
    )
    $summary = @()
    foreach ($experiment in $experiments) {
        $archive = Join-Path $OutputDirectory $experiment.name
        $flags = @("-PstartupOptimizations=$($experiment.lazy)", "-PoptimizeBuild=$($experiment.r8)",
            "-PappProfiles=$($experiment.profiles)", "-PstartupProfile=$($experiment.startup)")
        $existingReport = @(Get-ChildItem $archive -Recurse -Filter '*benchmarkData.json' -ErrorAction SilentlyContinue)
        $reuseResult = $Resume -and $existingReport.Count -eq 1
        if (-not $reuseResult) {
          Invoke-StudyGradle $experiment.name (@(
            ':macrobenchmark:connectedBenchmarkAndroidTest',
            "-Pandroid.testInstrumentationRunnerArguments.class=com.example.foodapp.macrobenchmark.StartupBenchmark#$($experiment.method)",
            '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark'
        ) + $flags)
        New-Item -ItemType Directory -Force $archive | Out-Null
        # robocopy supports the long filenames emitted by Android test reporting on Windows.
        & robocopy $additionalOutput $archive /E /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null
        if ($LASTEXITCODE -ge 8) { throw "Could not archive results for $($experiment.name)." }
        }
        if ($reuseResult) { Write-Host "Reusing archived results for $($experiment.name)." }
        $experiment | ConvertTo-Json | Set-Content (Join-Path $archive 'configuration.json') -Encoding UTF8
        $report = @(Get-ChildItem $archive -Recurse -Filter '*benchmarkData.json')
        if ($report.Count -ne 1) { throw "Expected one JSON report for $($experiment.name)." }
        $data = Get-Content -LiteralPath $report[0].FullName -Raw | ConvertFrom-Json
        foreach ($metric in $data.benchmarks[0].metrics.PSObject.Properties) {
            $summary += [pscustomobject]@{
                experiment = $experiment.name
                metric = $metric.Name
                medianMs = $metric.Value.median
                minimumMs = $metric.Value.minimum
                maximumMs = $metric.Value.maximum
                meanMs = ($metric.Value.runs | Measure-Object -Average).Average
                apkBytes = if ($reuseResult -and (Test-Path (Join-Path $archive 'apk-info.json'))) {
                    (Get-Content (Join-Path $archive 'apk-info.json') -Raw | ConvertFrom-Json).bytes
                } else { (Get-Item 'app\build\outputs\apk\benchmark\app-benchmark.apk').Length }
                runs = $metric.Value.runs.Count
            }
        }
        if (-not $reuseResult) {
            Copy-Item -LiteralPath 'app\build\outputs\apk\benchmark\app-benchmark.apk' -Destination (Join-Path $archive 'app-benchmark.apk')
            @{ bytes=(Get-Item 'app\build\outputs\apk\benchmark\app-benchmark.apk').Length;
               sha256=(Get-FileHash 'app\build\outputs\apk\benchmark\app-benchmark.apk' -Algorithm SHA256).Hash
            } | ConvertTo-Json | Set-Content (Join-Path $archive 'apk-info.json') -Encoding UTF8
        }
        $summary | Export-Csv (Join-Path $OutputDirectory 'summary.csv') -NoTypeInformation -Encoding UTF8
        $summary | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $OutputDirectory 'summary.json') -Encoding UTF8
    }
    # Leave the fully profiled F build ready for demo, rather than the last G experiment.
    $deliveryExperiment = $experiments | Where-Object { $_.name -eq 'F-baseline-startup-aot' }
    Invoke-StudyGradle 'optimized-smoke' @(
        ':macrobenchmark:connectedBenchmarkAndroidTest',
        '-Pandroid.testInstrumentationRunnerArguments.class=com.example.foodapp.macrobenchmark.LoginSmokeTest',
        "-PstartupOptimizations=$($deliveryExperiment.lazy)",
        "-PoptimizeBuild=$($deliveryExperiment.r8)",
        "-PappProfiles=$($deliveryExperiment.profiles)",
        "-PstartupProfile=$($deliveryExperiment.startup)"
    )
    $summary | Format-Table -AutoSize
    Write-Host "Results and per-iteration traces: $OutputDirectory"
} finally {
    Pop-Location
}
