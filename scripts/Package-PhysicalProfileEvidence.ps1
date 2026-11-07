param(
    [string]$OutputFile = 'build/artifacts/physical-profile-evidence.zip'
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $stage=Join-Path $root ('build/artifacts/physical-profile-evidence-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    New-Item -ItemType Directory -Path $stage | Out-Null
    function Copy-Evidence([string]$Source,[string]$RelativeDestination){
        $destination=Join-Path $stage $RelativeDestination
        New-Item -ItemType Directory -Force (Split-Path -Parent $destination) | Out-Null
        Copy-Item -LiteralPath $Source -Destination $destination
    }
    foreach($file in @('docs/physical-profile-results.md','docs/profile-refinement-notes.md','docs/startup-profile-applied.md')){Copy-Evidence $file $file}
    $measurements='docs/measurements/2026-11-07/physical-profiles'
    Get-ChildItem $measurements -Recurse -File | ForEach-Object {
        Copy-Evidence $_.FullName ($_.FullName.Substring($root.Length+1))
    }
    Copy-Evidence 'build/startup-study/profile-refined-pair-20261107/E-baseline-aot/app-benchmark.apk' 'apks/baseline-only.apk'
    Copy-Evidence 'build/startup-study/profile-refined-pair-20261107/F-baseline-startup-aot/app-benchmark.apk' 'apks/startup-generated.apk'
    Copy-Evidence 'build/startup-study/startup-phase-experiment-20261107/app-benchmark.apk' 'apks/startup-S-phase.apk'
    Copy-Evidence 'build/startup-study/physical-api33-pair-normalized-20261107/E-baseline-aot/app-benchmark.apk' 'apks/phone-captured-baseline.apk'
    Copy-Evidence 'build/startup-study/physical-api33-pair-normalized-20261107/F-baseline-startup-aot/app-benchmark.apk' 'apks/phone-captured-baseline-startup.apk'
    Copy-Evidence 'build/startup-study/physical-initial-display-20261107/pair/E-baseline-aot/app-benchmark.apk' 'apks/initial-display-baseline.apk'
    Copy-Evidence 'build/startup-study/physical-initial-display-20261107/pair/F-baseline-startup-aot/app-benchmark.apk' 'apks/initial-display-baseline-startup.apk'
    Copy-Evidence 'build/startup-study/profile-refined-pair-20261107/baseline-prof.txt' 'profiles/baseline-prof.txt'
    Copy-Evidence 'build/startup-study/profile-refined-pair-20261107/startup-prof.txt' 'profiles/startup-generated-prof.txt'
    Copy-Evidence 'build/startup-study/startup-phase-experiment-20261107/startup-prof.txt' 'profiles/startup-S-prof.txt'
    Copy-Evidence 'build/startup-study/physical-api33-pair-normalized-20261107/baseline-prof.txt' 'profiles/phone-baseline-prof.txt'
    Copy-Evidence 'build/startup-study/physical-api33-pair-normalized-20261107/startup-prof.txt' 'profiles/phone-startup-prof.txt'
    Copy-Evidence 'build/startup-study/physical-initial-display-20261107/pair/baseline-prof.txt' 'profiles/initial-display-baseline-prof.txt'
    Copy-Evidence 'build/startup-study/physical-initial-display-20261107/pair/startup-prof.txt' 'profiles/initial-display-startup-prof.txt'
    foreach($file in @('app/build.gradle.kts','app/proguard-rules.pro',
        'macrobenchmark/src/main/java/com/example/foodapp/macrobenchmark/StartupBenchmark.kt',
        'macrobenchmark/src/main/java/com/example/foodapp/macrobenchmark/BaselineProfileGenerator.kt',
        'macrobenchmark/src/main/java/com/example/foodapp/macrobenchmark/LoginSmokeTest.kt',
        'scripts/Confirm-ProfileEffects.ps1','scripts/Export-ProfileEffects.ps1',
        'scripts/audit_profile_study.py','scripts/verify_startup_profile.py','scripts/filter_startup_phase.py',
        'scripts/Capture-PhoneStartupProfile.ps1','scripts/Build-ProfilePair.ps1','scripts/merge_captured_profiles.py',
        'scripts/analyze_profile_traces.py','scripts/profile-trace-phases.sql','scripts/profile-startup-scheduling.sql')){
        Copy-Evidence $file ('sources/'+$file)
    }
    @'
# Physical profile evidence

Open docs/physical-profile-results.md first. Recorded sessions are kept separate:
resumed = 27 starts (D/E/F); startup-s-phase = 20 starts (E/F S-phase).
phone-captured = 27 starts (D/E/F with profiles captured on the physical phone).
IMPORTANT: phone-captured was confounded by user phone use; see conditions-amendment.json.
phone-captured-idle = the repeat with the same APKs after the user agreed to leave the phone untouched.
initial-display/exploratory = 20 starts (EFFE); Startup capture flushed immediately after initial display.
initial-display/confirmation = 20 new starts with the same APKs and reversed FEEF order.
Keep exploratory and confirmation statistics separate; read both before drawing conclusions.
Do not pool sessions or compare percentages using a control from a different session.
paused contains the earlier incomplete session, retained for transparency.

APK names describe the measured variants, not a guarantee of better performance.
JSON reports, raw rows, preparation logs, charts and independent audit reports are included.
Full Perfetto traces remain in the workspace build/startup-study directories; they are not in this ZIP.
The audit checksums refer to the original reports; rerunning the full audit also requires those traces.

Source excerpts and raw profiles are provided for review. Use the full food-app repository to build/run.
The learning-guide README.md and README.html were excluded to preserve the user's reading copies.
'@ | Set-Content (Join-Path $stage 'INSTRUCTIONS.md') -Encoding UTF8
    Get-ChildItem $stage -Recurse -File | ForEach-Object {
        [pscustomobject]@{path=$_.FullName.Substring($stage.Length+1);bytes=$_.Length;sha256=(Get-FileHash $_.FullName -Algorithm SHA256).Hash}
    } | ConvertTo-Json -Depth 3 | Set-Content (Join-Path $stage 'manifest.json') -Encoding UTF8
    $absoluteOutput=[IO.Path]::GetFullPath($OutputFile)
    Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $absoluteOutput -Force
    Get-Item $absoluteOutput | Select-Object FullName,Length
}finally{Pop-Location}
