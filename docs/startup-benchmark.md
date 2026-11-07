# Do khoi dong ung dung

> Ghi chép lịch sử bước thiết lập ban đầu. Quy trình hiện tại có TTFD, profile
> generator và sáu cấu hình so sánh: xem [startup-study.md](startup-study.md).

## Pham vi hien tai

- Module `macrobenchmark`, bai do `StartupBenchmark.coldStartupNoCompilation`.
- 10 lan khoi dong lanh, `StartupTimingMetric`, `CompilationMode.None()`.
- Ban app `benchmark` ke thua release hien tai (van tat toi uu build), khong debug,
  ky bang debug key, co ProfileInstaller va cho phep profiling.
- Package rieng: `com.example.jetpackcompose.benchmark`. Khong dung du lieu dang nhap
  hay gio hang cua app debug `com.example.jetpackcompose`.
- Kich ban chua dang nhap; xac nhan man hinh dang nhap xuat hien moi lan.
- Chi do TTID. Lenh cho man hinh dang nhap trong test khong phai phep do TTFD.
  Chua them Baseline/Startup Profiles hay thay doi luong khoi tao.

## Chay trong PowerShell

Mo Terminal tai thu muc goc du an, giu dung mot thiet bi ket noi va mo khoa man hinh.
Neu Java chua co trong PATH:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
```

Do tren dien thoai that:

```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
```

Chay thu quy trinh tren may ao (chi bo qua canh bao EMULATOR, khong bo qua loi khac):

```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR'
```

App benchmark duoc build va cai tu dong. Khong dang nhap vao ban app nay.
Neu da dang nhap nham, xoa du lieu CHI ban benchmark truoc khi chay lai:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm clear com.example.jetpackcompose.benchmark
```

## Doc ket qua

Ket qua JSON va `.perfetto-trace` nam duoi:

```text
macrobenchmark/build/outputs/connected_android_test_additional_output/
```

Tim file ket qua trong PowerShell:

```powershell
Get-ChildItem .\macrobenchmark\build\outputs -Recurse -Include '*benchmarkData.json','*.perfetto-trace'
```

Mo trace bang File > Open trong Android Studio. Giu nguyen JSON, trace va thong tin
thiet bi/build/commit cho moi dot do. Khong dung so tren may ao lam bang chung ve
hieu nang dien thoai that. Khong so truc tiep benchmark nay voi phep do ADB ban debug.

## So lieu lam quen truoc khi thiet lap benchmark

Nguoi dung do bang `adb shell am start -S -W` tren app debug, may ao API 37:

- Lan dau: 2333 ms.
- Tam lan tiep: 1576, 1436, 1521, 1486, 1501, 1486, 1496, 1515 ms.
- Trung binh tam lan: 1502.125 ms; trung vi: 1498.5 ms.
- Day la so lieu tham khao, khong phai ket qua toi uu. Chua xac dinh nguyen nhan
  lan dau cao hon; khong tu y loai khoi du lieu goc.

## Buoc nghien cuu tiep theo

1. Chay ban moc tren dien thoai that va luu ket qua lap lai.
2. Tach thi nghiem bat toi uu build khoi thi nghiem profile.
3. Them bo sinh Baseline Profiles, sau do so sanh cac compilation mode tuong ung.
4. Do Startup Profiles tren cac ban build voi cau hinh R8 giong nhau.
5. Neu do TTFD/man hinh chinh, dinh nghia moc san sang, them bao cao fully-drawn,
   va kiem soat tai khoan, quyen, cache, du lieu va do tre backend.

Tai lieu: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
