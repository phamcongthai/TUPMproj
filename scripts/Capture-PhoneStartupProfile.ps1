param(
    [Parameter(Mandatory=$true)][string]$Apk,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [Parameter(Mandatory=$true)][string]$Serial,
    [ValidateRange(3,15)][int]$MaxIterations=10,
    [ValidateSet('LoginSettled','InitialDisplay')][string]$CaptureWindow='LoginSettled',
    [switch]$Resume
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if((Test-Path $OutputDirectory) -and @(Get-ChildItem $OutputDirectory -Force).Count -and -not $Resume){throw 'Choose an empty output directory or resume the same capture.'}
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    $adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
    $env:ANDROID_SERIAL=$Serial
    $ConfirmBenchmarkInstallDialogs=$true
    # Reuse only the two reviewed installation helpers, not the measurement script body.
    $tokens=$null;$parseErrors=$null
    $ast=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'Confirm-ProfileEffects.ps1'),[ref]$tokens,[ref]$parseErrors)
    if($parseErrors){throw 'Cannot read installation helpers.'}
    foreach($name in @('Get-InstalledApkHash','Install-BenchmarkTarget')){
        $definition=$ast.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $name},$true)
        if(-not $definition){throw "Missing helper $name"}
        . ([scriptblock]::Create($definition.Extent.Text))
    }
    $package='com.example.jetpackcompose.benchmark'
    $stable=0;$previous=$null;$iterations=@()
    $firstIteration=1
    if($Resume){
        $old=Get-Content (Join-Path $OutputDirectory 'capture.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        if($old.serial -ne $Serial -or $old.captureWindow -ne $CaptureWindow -or (Get-InstalledApkHash $package) -ne (Get-FileHash $Apk).Hash){throw 'Resume requires the same device, installed capture APK and capture window.'}
        $iterations=Get-Content (Join-Path $OutputDirectory 'iterations.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        $last=$iterations[-1];$stable=[int]$last.stableCount;$firstIteration=[int]$last.iteration+1
        $lastPath=Join-Path $OutputDirectory ("iteration-{0:D2}-prof.txt" -f ([int]$last.iteration))
        $lines=@(Get-Content $lastPath -Encoding UTF8 | Where-Object {$_ -match '^[HSP]*L' -and -not $_.Contains('+')})
        $previous=(@($lines | Sort-Object -CaseSensitive -Unique) -join "`n")
        if($firstIteration -gt $MaxIterations){throw 'Increase MaxIterations to continue capture.'}
    }else{
        if(Get-InstalledApkHash $package){& $adb uninstall $package | Out-Null; if($LASTEXITCODE -ne 0){throw 'Cannot reset dedicated benchmark package.'}}
        Install-BenchmarkTarget $Apk $OutputDirectory
        $skip=& $adb shell am broadcast -a androidx.profileinstaller.action.SKIP_FILE -e EXTRA_SKIP_FILE_OPERATION WRITE_SKIP_FILE -n "$package/androidx.profileinstaller.ProfileInstallReceiver"
        if(($skip -join '') -notmatch 'result=10\b'){throw 'Cannot disable automatic profile installation.'}
        & $adb shell am force-stop $package
        $verify=& $adb shell cmd package compile -f -m verify $package
        if(($verify -join '') -notmatch 'Success'){throw 'Cannot prepare capture without AOT.'}
    }
    for($i=$firstIteration;$i -le $MaxIterations;$i++){
        Write-Host "Capturing phone startup $i/$MaxIterations..."
        $launch=& $adb shell am start -S -W -n "$package/com.example.jetpackcompose.MainActivity"
        if(($launch -join '') -notmatch 'Status: ok'){throw 'Cannot launch capture app.'}
        # For DEX layout, flush immediately after am start -W reports initial display.
        # Avoid the five-second wait/UI dump adding later work to the recorded journey.
        if($CaptureWindow -eq 'LoginSettled'){
            Start-Sleep -Seconds 5
            $uiPreference=$ErrorActionPreference
            try{$ErrorActionPreference='Continue'; & $adb shell uiautomator dump --compressed /data/local/tmp/food-capture-ui.xml 2>$null | Out-Null}finally{$ErrorActionPreference=$uiPreference}
            [xml]$ui=((& $adb shell cat /data/local/tmp/food-capture-ui.xml) -join '')
            if(-not $ui.SelectSingleNode('//node[@text="Đăng nhập để tiếp tục"]')){throw 'Capture did not reach logged-out Login.'}
        }
        $save=& $adb shell am broadcast -a androidx.profileinstaller.action.SAVE_PROFILE -n "$package/androidx.profileinstaller.ProfileInstallReceiver"
        if(($save -join '') -notmatch 'result=12\b'){throw 'ART profile save not signalled.'}
        Start-Sleep -Seconds 1
        & $adb shell am force-stop $package
        $compile=& $adb shell cmd package compile -f -m speed-profile $package
        if(($compile -join '') -notmatch 'Success'){throw 'Cannot merge observed capture profile.'}
        & $adb shell pm dump-profiles --dump-classes-and-methods $package | Out-Null
        if($LASTEXITCODE -ne 0){throw 'Cannot dump observed profile on this device.'}
        $raw=Join-Path $OutputDirectory ("iteration-{0:D2}-prof.txt" -f $i)
        & $adb pull "/data/misc/profman/$package-primary.prof.txt" $raw | Out-Null
        if($LASTEXITCODE -ne 0){throw 'Cannot retrieve captured profile.'}
        # Match AndroidX filterProfileRulesToTargetP: exclude array and inline-cache rules.
        $lines=@(Get-Content $raw -Encoding UTF8 | Where-Object {$_ -match '^[HSP]*L' -and -not $_.Contains('+')})
        if($lines.Count -lt 100){throw 'Captured profile is unexpectedly small.'}
        if(-not ($lines -match 'Lcom/example/jetpackcompose/MainActivity;')){throw 'App entrypoint absent from captured profile.'}
        $canonical=(@($lines | Sort-Object -CaseSensitive -Unique) -join "`n")
        if($canonical -ceq $previous){$stable++}else{$stable=1}
        $previous=$canonical
        $iterations+=[pscustomobject]@{iteration=$i;rules=$lines.Count;stableCount=$stable;sha256=(Get-FileHash $raw).Hash;launch=$launch;save=$save}
        $iterations | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $OutputDirectory 'iterations.json') -Encoding UTF8
        if($stable -ge 3){break}
    }
    foreach($kind in @('baseline','startup')){
        [IO.File]::WriteAllText([IO.Path]::GetFullPath((Join-Path $OutputDirectory "$kind-prof.txt")),($lines -join "`n")+"`n",(New-Object Text.UTF8Encoding($false)))
    }
    @{
        serial=$Serial;fingerprint=(& $adb shell getprop ro.build.fingerprint).Trim()
        captureApkSha256=(Get-FileHash $Apk).Hash;iterations=$iterations.Count;stableIterations=$stable
        stable=($stable -ge 3);scenario='Actual Android 13 logged-out launch to Login; observed ART rules only'
        captureWindow=$CaptureWindow
        initialDisplayBoundary=if($CaptureWindow -eq 'InitialDisplay'){'am start -W completed; SAVE_PROFILE immediately; asynchronous save requires a one-second flush wait'}else{'Five-second wait followed by Login UI verification'}
        method='Fresh host-side install replaces unavailable non-root reset; skip ProfileInstaller; verify; startup; SAVE_PROFILE; speed-profile merge; pm dump-profiles HRF'
        sameJourneyInBothProfiles=$true
    } | ConvertTo-Json | Set-Content (Join-Path $OutputDirectory 'capture.json') -Encoding UTF8
    Write-Host "Phone profile captured ($($lines.Count) rules; stable $stable): $OutputDirectory"
}finally{Pop-Location}
