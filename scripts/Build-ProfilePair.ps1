param(
    [Parameter(Mandatory=$true)][string]$CaptureDirectory,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [string]$Python = 'build/guide-tools/python/python.exe'
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if ((Test-Path $OutputDirectory) -and @(Get-ChildItem $OutputDirectory -Force).Count) { throw 'Choose an empty output directory.' }
    foreach($kind in @('baseline','startup')) {
        if (-not (Test-Path (Join-Path $CaptureDirectory "$kind-prof.txt"))) { throw 'Profile capture must complete first.' }
    }
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    & $Python scripts/merge_captured_profiles.py (Join-Path $CaptureDirectory 'baseline-prof.txt') (Join-Path $CaptureDirectory 'startup-prof.txt') app/src/main/profiles/baseline/baseline-prof.txt |
        Set-Content (Join-Path $OutputDirectory 'profile-merge.json') -Encoding UTF8
    if($LASTEXITCODE -ne 0){throw 'Cannot merge captured profiles.'}
    Copy-Item (Join-Path $CaptureDirectory 'startup-prof.txt') app/src/main/profiles/startup/startup-prof.txt
    Copy-Item app/src/main/profiles/baseline/baseline-prof.txt (Join-Path $OutputDirectory 'baseline-prof.txt')
    Copy-Item app/src/main/profiles/startup/startup-prof.txt (Join-Path $OutputDirectory 'startup-prof.txt')
    $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
    foreach($case in @(@{name='E-baseline-aot';startup='false'},@{name='F-baseline-startup-aot';startup='true'})) {
        $folder=Join-Path $OutputDirectory $case.name
        New-Item -ItemType Directory -Force $folder | Out-Null
        Write-Host "Building $($case.name)..."
        $previous=$ErrorActionPreference
        try {
            $ErrorActionPreference='Continue'
            & .\gradlew.bat :app:assembleBenchmark :macrobenchmark:assembleBenchmark -PstartupOptimizations=true -PoptimizeBuild=true -PappProfiles=true "-PstartupProfile=$($case.startup)" --console=plain --max-workers=2 *> (Join-Path $folder 'build.log')
            $code=$LASTEXITCODE
        }finally{$ErrorActionPreference=$previous}
        if($code -ne 0){throw "Build failed: $($case.name)"}
        Copy-Item app/build/outputs/apk/benchmark/app-benchmark.apk (Join-Path $folder 'app-benchmark.apk')
        Copy-Item app/build/intermediates/r8_metadata/benchmark/minifyBenchmarkWithR8/r8-metadata.dat (Join-Path $folder 'r8-metadata.json')
        Copy-Item app/build/outputs/mapping/benchmark/mapping.txt (Join-Path $folder 'mapping.txt')
        if($case.startup -eq 'true') {
            & $Python scripts/verify_startup_profile.py --apk (Join-Path $folder 'app-benchmark.apk') --metadata (Join-Path $folder 'r8-metadata.json') --output (Join-Path $folder 'startup-verification.json')
            if($LASTEXITCODE -ne 0){throw 'Startup DEX verification failed.'}
        }
    }
    Get-ChildItem app/src,macrobenchmark/src,scripts -Recurse -File | ForEach-Object {
        [pscustomobject]@{path=$_.FullName.Substring($root.Length+1);sha256=(Get-FileHash $_.FullName).Hash}
    } | Export-Csv (Join-Path $OutputDirectory 'source-hashes.csv') -NoTypeInformation -Encoding UTF8
    Write-Host "Controlled APK pair ready: $OutputDirectory"
}finally{Pop-Location}
