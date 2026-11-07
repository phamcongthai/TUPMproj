param([string]$OutputDirectory = 'build\artifacts')
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    $OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    $stage = Join-Path $OutputDirectory ('guide-staging-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    $bundle = Join-Path $stage 'android-startup-profiles-guide'
    New-Item -ItemType Directory -Force $bundle | Out-Null
    Copy-Item docs\android-startup-guide\* $bundle -Recurse
    if (@(Get-ChildItem (Join-Path $bundle 'assets') -Filter '0*.png').Count -ne 8) { throw 'Expected 8 rendered diagrams.' }
    New-Item -ItemType Directory -Force (Join-Path $bundle 'evidence') | Out-Null
    foreach ($name in @('confirmed-pair', 'swiftshader-study', 'startup-profile-confirmation')) {
        Copy-Item "docs\measurements\2026-11-07\$name" (Join-Path $bundle 'evidence') -Recurse
    }
    Copy-Item docs\measurements\2026-11-07\source-hashes-at-delivery.csv (Join-Path $bundle 'evidence\historical-source-hashes.csv')
    Copy-Item docs\report-kit\results (Join-Path $bundle 'results') -Recurse
    Copy-Item docs\report-kit\report-outline.md,docs\report-kit\demo-and-defense.md $bundle
    $files = @(
        'app/build.gradle.kts', 'app/proguard-rules.pro', 'build.gradle.kts', 'settings.gradle.kts',
        'gradle/libs.versions.toml', 'app/src/benchmark/AndroidManifest.xml',
        'app/src/main/AndroidManifest.xml', 'macrobenchmark/build.gradle.kts',
        'app/src/main/java/com/example/jetpackcompose/MainActivity.kt',
        'app/src/main/java/com/example/jetpackcompose/navigation/AppNavigation.kt',
        'app/src/main/java/com/example/jetpackcompose/viewmodel/AuthViewModel.kt',
        'app/src/main/java/com/example/jetpackcompose/data/repository/AuthRepository.kt',
        'app/src/main/java/com/example/jetpackcompose/data/datastore/AuthPreference.kt',
        'app/src/main/java/com/example/jetpackcompose/di/NetworkModule.kt',
        'app/src/main/java/com/example/jetpackcompose/presentation/screen/splash/SplashScreen.kt',
        'app/src/main/java/com/example/jetpackcompose/presentation/screen/login/LoginScreen.kt',
        'app/src/main/java/com/example/jetpackcompose/presentation/screen/main/MainScreen.kt'
    )
    $files += @(Get-ChildItem macrobenchmark/src,app/src/main/profiles -Recurse -File | ForEach-Object { $_.FullName.Substring($projectDirectory.Length+1) })
    $files += @(Get-ChildItem scripts -Filter '*.ps1' | ForEach-Object { $_.FullName.Substring($projectDirectory.Length+1) })
    $files += 'scripts/verify_startup_profile.py'
    $manifest = foreach ($file in $files) {
        $destination = Join-Path $bundle "source\$file"
        New-Item -ItemType Directory -Force (Split-Path -Parent $destination) | Out-Null
        Copy-Item -LiteralPath $file -Destination $destination
        [pscustomobject]@{path=$file;sha256=(Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash}
    }
    $manifest | Export-Csv (Join-Path $bundle 'source-manifest.csv') -NoTypeInformation -Encoding UTF8
    @{
        generatedAt=[DateTimeOffset]::Now.ToString('o'); originalRepository='https://github.com/leloi04/food-app'
        baseCommit=(& git rev-parse HEAD); sourceSnapshot='Selected files for reading; not a complete buildable checkout'
        measurements='Historical 2026-11-07 SwiftShader + confirmed ABBA JSON; no new device measurements during packaging'
        traces='Full Perfetto traces retained in workspace build/startup-study; not included here'
    } | ConvertTo-Json | Set-Content (Join-Path $bundle 'bundle.json') -Encoding UTF8
    @'
# Source snapshot

Selected current source files to read alongside the guide. Original app:
https://github.com/leloi04/food-app (base commit 17aa98a).

This is not a complete Gradle checkout. Run builds and PowerShell study scripts
from the complete food-app repository. No SDK, credentials, signing secrets,
local.properties or IDE settings are packaged. source-manifest.csv identifies
this delivery snapshot; historical-source-hashes.csv identifies the earlier
measurement delivery. Documentation/script updates do not imply remeasurement.
'@ | Set-Content (Join-Path $bundle 'source\README.md') -Encoding UTF8
    $zip = Join-Path $OutputDirectory 'android-startup-profiles-guide.zip'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip }
    [IO.Compression.ZipFile]::CreateFromDirectory($stage, $zip)
    Get-Item $zip | Select-Object FullName,Length
    Get-FileHash -LiteralPath $zip -Algorithm SHA256 | Format-List
} finally { Pop-Location }
