param(
    [string]$PairDirectory = 'docs\measurements\2026-11-07\confirmed-pair',
    [string]$StudyDirectory = 'docs\measurements\2026-11-07\swiftshader-study',
    [string]$OutputDirectory = 'docs\report-kit\results',
    [ValidateRange(3, 20)][int]$IterationsPerBlock = 5,
    [switch]$SkipCharts
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    $culture = [Globalization.CultureInfo]::InvariantCulture
    function Number([double]$Value) { $Value.ToString('F3', $culture) }
    function Median([double[]]$Values) {
        $sorted = @($Values | Sort-Object)
        if ($sorted.Count -eq 0) { throw 'Empty sample.' }
        $middle = [int][Math]::Floor($sorted.Count / 2)
        if ($sorted.Count % 2) { return $sorted[$middle] }
        return ($sorted[$middle - 1] + $sorted[$middle]) / 2
    }
    $rows = @()
    $inputs = @()
    $fingerprint = $null
    foreach ($phase in @('A1', 'E1', 'E2', 'A2')) {
        $file = Join-Path $PairDirectory "$phase\benchmarkData.json"
        $data = Get-Content -LiteralPath $file -Raw -Encoding UTF8 | ConvertFrom-Json
        $expectedMethod = if ($phase.StartsWith('A')) { 'coldStartupNoCompilation' } else { 'coldStartupWithBaselineProfile' }
        if (@($data.benchmarks).Count -ne 1 -or $data.benchmarks[0].name -ne $expectedMethod) {
            throw "Wrong benchmark identity: $file"
        }
        if ($fingerprint -and $fingerprint -ne $data.context.build.fingerprint) { throw 'Device fingerprint changed between blocks.' }
        $fingerprint = $data.context.build.fingerprint
        $inputs += [pscustomobject]@{ path=$file; sha256=(Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
        foreach ($metric in @('timeToInitialDisplayMs', 'timeToFullDisplayMs')) {
            $runs = @($data.benchmarks[0].metrics.$metric.runs)
            if ($runs.Count -ne $IterationsPerBlock) { throw "Unexpected run count: $phase / $metric" }
            for ($i=0; $i -lt $runs.Count; $i++) {
                $value = [double]$runs[$i]
                if ([double]::IsNaN($value) -or [double]::IsInfinity($value) -or $value -le 0) { throw 'Invalid timing.' }
                $rows += [pscustomobject]@{ configuration=$phase.Substring(0,1); block=$phase; iteration=$i+1; metric=$metric; milliseconds=$value }
            }
        }
    }
    $statistics = @()
    foreach ($configuration in @('A','E')) {
        foreach ($metric in @('timeToInitialDisplayMs', 'timeToFullDisplayMs')) {
            $values = @($rows | Where-Object { $_.configuration -eq $configuration -and $_.metric -eq $metric } | ForEach-Object { $_.milliseconds })
            $measure = $values | Measure-Object -Minimum -Maximum -Average
            $statistics += [pscustomobject]@{
                configuration=$configuration; metric=$metric; n=$values.Count
                medianMs=(Median $values); meanMs=$measure.Average; minimumMs=$measure.Minimum; maximumMs=$measure.Maximum
            }
        }
    }
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    $rows | Export-Csv (Join-Path $OutputDirectory 'raw-runs.csv') -NoTypeInformation -Encoding UTF8
    $statistics | Export-Csv (Join-Path $OutputDirectory 'statistics.csv') -NoTypeInformation -Encoding UTF8
    $statistics | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $OutputDirectory 'statistics.json') -Encoding UTF8
    $inputs | Export-Csv (Join-Path $OutputDirectory 'input-hashes.csv') -NoTypeInformation -Encoding UTF8
    $lines = @('# Results recomputed from raw Macrobenchmark JSON', '',
        'ABBA: A1 -> E1 -> E2 -> A2. Logged-out cold start. API 37 SwiftShader emulator (recorded study).',
        'All observations retained; median calculated over both blocks together.', '',
        '| Metric | A median (ms) | E median (ms) | Reduction | A min-max | E min-max |',
        '|---|---:|---:|---:|---:|---:|')
    foreach ($metric in @('timeToInitialDisplayMs', 'timeToFullDisplayMs')) {
        $a = $statistics | Where-Object { $_.configuration -eq 'A' -and $_.metric -eq $metric }
        $e = $statistics | Where-Object { $_.configuration -eq 'E' -and $_.metric -eq $metric }
        $reduction = (100 * ($a.medianMs - $e.medianMs) / $a.medianMs).ToString('F2', $culture)
        $lines += "| $metric | $(Number $a.medianMs) | $(Number $e.medianMs) | $reduction% | $(Number $a.minimumMs)-$(Number $a.maximumMs) | $(Number $e.minimumMs)-$(Number $e.maximumMs) |"
    }
    $apkRows = @()
    foreach ($entry in @(@{id='A'; name='A-eager-unoptimized'}, @{id='E'; name='E-baseline-aot'})) {
        $file = Join-Path $StudyDirectory "$($entry.name)\apk-info.json"
        $info = Get-Content -LiteralPath $file -Raw -Encoding UTF8 | ConvertFrom-Json
        $apkRows += [pscustomobject]@{configuration=$entry.id; bytes=[long]$info.bytes; sha256=$info.sha256}
    }
    $apkRows | Export-Csv (Join-Path $OutputDirectory 'apk-sizes.csv') -NoTypeInformation -Encoding UTF8
    $sizeReduction = (100 * (1 - $apkRows[1].bytes / [double]$apkRows[0].bytes)).ToString('F2', $culture)
    $lines += @('', "APK A: $($apkRows[0].bytes) bytes; E: $($apkRows[1].bytes) bytes; reduction: $sizeReduction%.", '',
        'E combines deferred initialization, R8/resource shrinking and Baseline Profile AOT.',
        'This comparison does not isolate the effect of any single technique.',
        'Ranges overlap. No statistical significance or physical-device speedup is established.',
        'The chart lines mark medians, not confidence intervals. Points are individual starts.',
        'This historical A/E comparison excludes Startup Profile. The current default F also enables Startup Profile; see the newer A/D/E/F confirmation.')
    $lines | Set-Content (Join-Path $OutputDirectory 'results.md') -Encoding UTF8
    if (-not $SkipCharts) {
        Add-Type -AssemblyName System.Windows.Forms.DataVisualization
        Add-Type -AssemblyName System.Drawing
        foreach ($metric in @('timeToInitialDisplayMs', 'timeToFullDisplayMs')) {
            $short = if ($metric -eq 'timeToInitialDisplayMs') { 'TTID' } else { 'TTFD' }
            $chart = New-Object System.Windows.Forms.DataVisualization.Charting.Chart
            try {
                $chart.Width = 1400; $chart.Height = 900
                $chart.BackColor = [Drawing.Color]::White
                $area = New-Object System.Windows.Forms.DataVisualization.Charting.ChartArea 'Timing'
                $area.AxisX.Minimum=0.5; $area.AxisX.Maximum=2.5; $area.AxisX.Interval=1
                $area.AxisX.CustomLabels.Add(0.5,1.5,'A - control') | Out-Null
                $area.AxisX.CustomLabels.Add(1.5,2.5,'E - optimized') | Out-Null
                $area.AxisX.MajorGrid.Enabled=$false
                $area.AxisY.Minimum=0; $area.AxisY.Title='Cold startup time (ms)'
                $area.AxisY.MajorGrid.LineColor=[Drawing.Color]::Gainsboro
                $area.AxisX.LabelStyle.Font=New-Object Drawing.Font('Arial',14)
                $area.AxisY.LabelStyle.Font=New-Object Drawing.Font('Arial',12)
                $area.AxisY.TitleFont=New-Object Drawing.Font('Arial',14)
                $chart.ChartAreas.Add($area)
                $title=$chart.Titles.Add("$short | individual starts and medians")
                $title.Font=New-Object Drawing.Font('Arial',20,[Drawing.FontStyle]::Bold)
                $subtitle=$chart.Titles.Add('API 37 SwiftShader emulator | ABBA | n = 10 per configuration | lower is faster')
                $subtitle.Font=New-Object Drawing.Font('Arial',13)
                $bottom=$chart.Titles.Add('Combined intervention; overlapping ranges; physical-device validation remains.')
                $bottom.Docking='Bottom'; $bottom.Font=New-Object Drawing.Font('Arial',12)
                $x=0
                foreach ($configuration in @('A','E')) {
                    $x++
                    $color=if($configuration -eq 'A'){[Drawing.Color]::FromArgb(80,100,130)}else{[Drawing.Color]::FromArgb(0,135,115)}
                    $points=New-Object System.Windows.Forms.DataVisualization.Charting.Series "Runs-$configuration"
                    $points.ChartType='Point'; $points.MarkerSize=11; $points.Color=$color
                    $values=@($rows | Where-Object { $_.configuration -eq $configuration -and $_.metric -eq $metric })
                    for($i=0;$i -lt $values.Count;$i++) {
                        $jitter=(($i % 5)-2)*0.035
                        $points.Points.AddXY(($x+$jitter),$values[$i].milliseconds) | Out-Null
                    }
                    $chart.Series.Add($points)
                    $stat=$statistics | Where-Object { $_.configuration -eq $configuration -and $_.metric -eq $metric }
                    $line=New-Object System.Windows.Forms.DataVisualization.Charting.Series "Median-$configuration"
                    $line.ChartType='Line'; $line.Color=$color; $line.BorderWidth=4
                    $line.Points.AddXY(($x-0.25),$stat.medianMs) | Out-Null
                    $pointIndex=$line.Points.AddXY(($x+0.25),$stat.medianMs)
                    $line.Points[$pointIndex].Label="median $(Number $stat.medianMs) ms"
                    $line.Font=New-Object Drawing.Font('Arial',12)
                    $chart.Series.Add($line)
                }
                $chart.SaveImage((Join-Path ([IO.Path]::GetFullPath($OutputDirectory)) "$short.png"), 'Png')
            } finally { $chart.Dispose() }
        }
    }
    $statistics | Format-Table -AutoSize
    Write-Host "Exported tables and figures: $OutputDirectory"
} finally { Pop-Location }
