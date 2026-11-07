# Baseline và Startup Profile đã được áp dụng vào app

`app/build.gradle.kts` mặc định bật `appProfiles` và `startupProfile` cho cấu hình tối ưu. Benchmark kế thừa release, không debug, bật R8/resource shrinking và cho phép shell profiling. Bản debug chạy thông thường trong Android Studio không đại diện cho tối ưu bố trí DEX này.

**APK hiện tại:** `build/artifacts/food-app-initial-display-startup-profile.apk`, package `com.example.jetpackcompose.benchmark`, SHA-256 `B92C7C1768E02A2485E429869BE89FA649B09034F183D0E332D4C76CA9763A67`. Đã cài đúng APK này trên điện thoại và biên dịch theo Baseline.

| Kiểm tra | Kết quả |
|---|---|
| R8 DEX layout và profile-guided optimization | true |
| DEX đầu đánh dấu startup, chứa MainActivity | Có |
| SHA-256 các DEX khớp metadata | PASS |
| Binary Baseline Profile trong APK | Có, 11.484 byte |
| ProfileInstaller / ART trên điện thoại | result 1 / speed-profile |
| Đăng nhập → Đăng ký → quay lại | PASS |

[Bằng chứng DEX](measurements/2026-11-07/physical-profiles/initial-display/startup-verification.json) · [Kiểm tra chức năng](measurements/2026-11-07/physical-profiles/initial-display/final-validation/result.json) · [Đồng nhất APK/runner/profile giữa hai phiên](measurements/2026-11-07/physical-profiles/initial-display/cross-study-check.json).

**Kết quả Startup mới:** hai phiên riêng 20 lượt/phiên, E/F cùng Baseline AOT và chỉ bật thêm Startup ở F. Phiên E–F–F–E giảm trung vị TTID 20,49%, TTFD 18,52%; phiên xác nhận đảo F–E–E–F giảm 7,66% và 11,05%. Lợi ích mô tả lặp lại nhưng mức giảm dao động; không gộp hai phiên hoặc khẳng định hiệu quả phổ quát/ý nghĩa thống kê.

**Kết quả Baseline riêng:** trong phiên D/E/F trước, D/E cùng APK, thay ART verify bằng Baseline + speed-profile cho TTID thấp hơn 12,95%, TTFD thấp hơn 22,86%. Bộ profile của phiên đó khác bộ mới; không dùng E cũ làm đối chứng cho Startup mới và không cộng phần trăm. [Báo cáo tổng kết và kết quả chưa đạt trước đây](physical-profile-results.md).

Startup mới được thu ngay sau khi `am start -W` hoàn thành hiển thị ban đầu, thay vì chờ 5 giây rồi kiểm tra UI trước khi lưu. Sau 11 vòng, ba bộ quy tắc cuối giống nhau. Baseline vẫn bao phủ Login đầy đủ và hợp nhất quy tắc mới. Không dùng thay đổi lazy initialization/R8 để quy công cho hai profile.

Startup được R8 dùng lúc build để bố trí DEX; không cần có `startup-prof.txt` trong APK. Baseline được đóng gói binary để ART biên dịch. Kiểm tra theo [hướng dẫn Android](https://developer.android.com/topic/performance/baselineprofiles/confirm-startup-profiles); chọn hành trình theo [phạm vi hiển thị ban đầu](https://developer.android.com/topic/performance/baselineprofiles/difference-baseline-startup).

Điện thoại API 33 này chặn reinstall nội bộ của benchmark. Script cài mới và chuẩn bị ART từ host, sau đó dùng `androidx.benchmark.compilation.enabled=false` theo [tùy chọn chính thức](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args#androidxbenchmarkcompilationenabled). Tên phương thức test không thay thế bằng chứng trạng thái ART trước mỗi khối.

Script: `Capture-PhoneStartupProfile.ps1` với `-CaptureWindow InitialDisplay -MaxIterations 15`; `Build-ProfilePair.ps1`; `Confirm-ProfileEffects.ps1`; `audit_profile_study.py`; `verify_startup_profile.py`. [Kế hoạch và lệnh tái lập](profile-refinement-notes.md).

Các APK cũ và kết quả bất lợi được giữ riêng. Hai README của tài liệu học không được chỉnh sửa trong phiên này. Cài đặt giữ sáng qua USB đã được trả về giá trị ban đầu 0.
