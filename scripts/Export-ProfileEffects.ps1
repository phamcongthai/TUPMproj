param(
    [Parameter(Mandatory=$true)][string]$InputDirectory,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [switch]$ProfilePairOnly,
    [switch]$ProfilesOnly
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $statistics=Get-Content (Join-Path $InputDirectory 'statistics.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $raw=@(Import-Csv (Join-Path $InputDirectory 'raw-runs.csv'))
    $apks=Get-Content (Join-Path $InputDirectory 'apk-info.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $culture=[Globalization.CultureInfo]::InvariantCulture
    $methodology=Get-Content (Join-Path $InputDirectory 'methodology.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $configurations=if($ProfilesOnly){@('D','E','F')}elseif($ProfilePairOnly){@('E','F')}else{@('A','D','E','F')}
    $order=$methodology.order -join '-'
    $expectedN=if($ProfilesOnly){3*$methodology.iterationsPerBlock}else{2*$methodology.iterationsPerBlock}
    function Format-Number([double]$value){$value.ToString('F3',$culture)}
    function Find-Stat([string]$config,[string]$metric){$statistics | Where-Object {$_.configuration -eq $config -and $_.metric -eq $metric}}
    New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
    $lines=@('# Xác nhận Baseline Profile và Startup Profile', '',
        "Cold start khi chưa đăng nhập; thiết bị $($methodology.fingerprint); thứ tự $order.",
        "Mỗi khối $($methodology.iterationsPerBlock) lần, gộp $expectedN lần/cấu hình; không build giữa các khối; giữ đủ mọi quan sát.", '',
        '| Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |', '|---|---:|---:|')
    $confounded=$false
    $conditionFile=Join-Path $InputDirectory 'conditions-amendment.json'
    if(Test-Path $conditionFile){
        $conditions=Get-Content $conditionFile -Raw -Encoding UTF8 | ConvertFrom-Json
        if($conditions.userConfirmedPhoneUseDuringStudy){
            $confounded=$true
            $lines=@($lines[0],'','**Phiên bị nhiễu: người dùng xác nhận có sử dụng điện thoại trong lúc đo. Giữ dữ liệu để tham khảo, không dùng để kết luận hiệu quả. Audit PASS không kiểm tra điều kiện để yên máy.**')+$lines[1..($lines.Count-1)]
        }
    }
    foreach($config in $configurations){
        $ttid=Find-Stat $config 'timeToInitialDisplayMs'
        $ttfd=Find-Stat $config 'timeToFullDisplayMs'
        if($ttid.n -ne $expectedN -or $ttfd.n -ne $expectedN){throw 'Run count does not match methodology.'}
        $lines+="| $config | $(Format-Number $ttid.medianMs) | $(Format-Number $ttfd.medianMs) |"
    }
    $lines+=''
    if('A' -in $configurations){$lines+='A: eager, không R8, None.'}
    if('D' -in $configurations){
        $lines+=if($methodology.externalCompilation){'D: lazy + R8, cùng APK E; cài mới và chuẩn bị verify (không AOT).'}else{'D: lazy + R8, cùng APK E; CompilationMode.None.'}
    }
    $lines+=if($methodology.externalCompilation){'E/F: cùng mã app, lazy, R8 và đầu vào Baseline. Cài mới, cài Baseline rồi biên dịch speed-profile; F bật thêm Startup Profile. Macrobenchmark compilation.enabled=false giữ trạng thái đã chuẩn bị; xem external-compilation.log và dexopt-before.txt từng khối.'}else{'E/F: cùng mã app, lazy, R8 và đầu vào Baseline; CompilationMode.Partial/Require. F bật thêm Startup Profile.'}
    $lines+=@('', '| So sánh | Ý nghĩa | TTID thay đổi | TTFD thay đổi |', '|---|---|---:|---:|')
    $pairs=if($ProfilePairOnly){@(@{before='E';after='F';meaning='Bổ sung Startup Profile'})}else{@(@{before='D';after='E';meaning='Biên dịch theo Baseline Profile'},@{before='E';after='F';meaning='Bổ sung Startup Profile'},@{before='A';after='F';meaning='Gói tối ưu đầy đủ so đối chứng'})}
    if($ProfilesOnly){$pairs=@($pairs | Where-Object {$_.before -ne 'A'})}
    foreach($pair in $pairs){
        $changes=foreach($metric in @('timeToInitialDisplayMs','timeToFullDisplayMs')){
            $before=Find-Stat $pair.before $metric
            $after=Find-Stat $pair.after $metric
            (100*($after.medianMs-$before.medianMs)/$before.medianMs).ToString('+0.00;-0.00;0.00',$culture)+'%'
        }
        $lines+="| $($pair.before)/$($pair.after) | $($pair.meaning) | $($changes[0]) | $($changes[1]) |"
    }
    $lines+=@('', 'Dấu âm = thời gian giảm; dấu dương = thời gian tăng. So sánh trung vị, không phải mọi lần đều nhanh/chậm hơn.', '',
        '| APK | Byte | SHA-256 |', '|---|---:|---|')
    foreach($apk in @($apks | Where-Object {$_.configuration -in $configurations})){$lines+="| $($apk.configuration) | $($apk.bytes) | $($apk.sha256) |"}
    $lines+=@('', '| Cấu hình | Chỉ số | n | Trung bình (ms) | Nhỏ nhất (ms) | Lớn nhất (ms) |', '|---|---|---:|---:|---:|---:|')
    foreach($config in $configurations){
        foreach($metric in @('timeToInitialDisplayMs','timeToFullDisplayMs')){
            $stat=Find-Stat $config $metric
            $label=if($metric -eq 'timeToInitialDisplayMs'){'TTID'}else{'TTFD'}
            $lines+="| $config | $label | $($stat.n) | $(Format-Number $stat.meanMs) | $(Format-Number $stat.minimumMs) | $(Format-Number $stat.maximumMs) |"
        }
    }
    $deviceNote=if($methodology.emulator){'Đo trên máy ảo; chưa xác nhận trên máy thật.'}else{'Đo trên một thiết bị thật; chưa khái quát sang các điện thoại khác.'}
    $lines+=@('', "$deviceNote Chưa thiết lập ý nghĩa thống kê.")
    if('D' -in $configurations){$lines+='D/E kiểm soát cùng APK nhưng compilation có thể sử dụng cả profile thư viện; không quy hết cho riêng quy tắc app.'}
    if('A' -in $configurations){$lines+='A/F là tác động tổng hợp; không phải mức cải thiện riêng do hai loại profile.'}
    $lines+='Việc Startup Profile được áp dụng được kiểm tra độc lập bằng metadata R8 và SHA-256 DEX, xem startup-verification.json.'
    $lines | Set-Content (Join-Path $OutputDirectory 'results.md') -Encoding UTF8
    Add-Type -AssemblyName System.Windows.Forms.DataVisualization
    Add-Type -AssemblyName System.Drawing
    foreach($metric in @('timeToInitialDisplayMs','timeToFullDisplayMs')){
        $short=if($metric -eq 'timeToInitialDisplayMs'){'TTID'}else{'TTFD'}
        $chart=New-Object System.Windows.Forms.DataVisualization.Charting.Chart
        try{
            $chart.Width=1500; $chart.Height=900; $chart.BackColor=[Drawing.Color]::White
            $area=New-Object System.Windows.Forms.DataVisualization.Charting.ChartArea 'Profile comparison'
            $area.AxisX.Minimum=0.5; $area.AxisX.Maximum=$configurations.Count+0.5; $area.AxisX.Interval=1
            $area.AxisX.MajorGrid.Enabled=$false
            $area.AxisY.Minimum=0; $area.AxisY.Title='Cold startup time (ms)'
            $area.AxisY.MajorGrid.LineColor=[Drawing.Color]::Gainsboro
            $area.AxisY.LabelStyle.Font=New-Object Drawing.Font('Arial',12)
            $area.AxisY.TitleFont=New-Object Drawing.Font('Arial',14)
            $chart.ChartAreas.Add($area)
            $title=$chart.Titles.Add("$short | $($configurations -join ' / ') profile comparison")
            $title.Font=New-Object Drawing.Font('Arial',20,[Drawing.FontStyle]::Bold)
            if($confounded){
                $warning=$chart.Titles.Add('CONFOUNDED: phone used during measurement; not evidence of a causal speedup')
                $warning.ForeColor=[Drawing.Color]::DarkRed
                $warning.Font=New-Object Drawing.Font('Arial',13,[Drawing.FontStyle]::Bold)
            }
            $subtitle=$chart.Titles.Add("$expectedN starts per configuration | order $order | points = starts, lines = medians")
            $subtitle.Font=New-Object Drawing.Font('Arial',13)
            $legend=@{A='A: eager/no R8';D='D: same E APK / verify';E='E: Baseline AOT';F='F: Baseline AOT + Startup Profile'}
            $footer=$chart.Titles.Add((@($configurations | ForEach-Object {$legend[$_]}) -join ' | '))
            $footer.Docking='Bottom'; $footer.Font=New-Object Drawing.Font('Arial',12)
            $colors=@('SlateGray','DarkOrange','Teal','RoyalBlue')
            $x=0
            foreach($config in $configurations){
                $x++
                $area.AxisX.CustomLabels.Add(($x-0.5),($x+0.5),$config) | Out-Null
                $series=New-Object System.Windows.Forms.DataVisualization.Charting.Series "Runs-$config"
                $series.ChartType='Point'; $series.MarkerSize=10; $series.Color=[Drawing.Color]::FromName($colors[$x-1])
                $runs=@($raw | Where-Object {$_.configuration -eq $config -and $_.metric -eq $metric})
                for($i=0;$i -lt $runs.Count;$i++){
                    $value=[double]::Parse($runs[$i].milliseconds,$culture)
                    $series.Points.AddXY(($x+(($i%5)-2)*0.035),$value) | Out-Null
                }
                $chart.Series.Add($series)
                $stat=Find-Stat $config $metric
                $line=New-Object System.Windows.Forms.DataVisualization.Charting.Series "Median-$config"
                $line.ChartType='Line'; $line.BorderWidth=4; $line.Color=$series.Color
                $line.Points.AddXY(($x-0.25),$stat.medianMs) | Out-Null
                $index=$line.Points.AddXY(($x+0.25),$stat.medianMs)
                $line.Points[$index].Label="$(Format-Number $stat.medianMs) ms"
                $line.Font=New-Object Drawing.Font('Arial',11)
                $chart.Series.Add($line)
            }
            $chart.SaveImage((Join-Path ([IO.Path]::GetFullPath($OutputDirectory)) "$short-profiles.png"),'Png')
        }finally{$chart.Dispose()}
    }
    Get-Content (Join-Path $OutputDirectory 'results.md') -Encoding UTF8
}finally{Pop-Location}
