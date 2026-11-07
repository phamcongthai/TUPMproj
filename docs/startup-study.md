# Tối ưu khởi động Food App: triển khai và thực nghiệm

Kết quả đã chạy: [confirmed-results.md](measurements/2026-11-07/confirmed-results.md).

## Phạm vi

Đề tài thuộc tối ưu hiệu năng và tài nguyên Android. Kịch bản tự động hiện tại là
người chưa đăng nhập mở app đến màn hình đăng nhập. Không cần tài khoản, không gửi
đơn hàng và không phụ thuộc thời gian phản hồi backend. Không suy rộng kết quả này
thành hiệu năng tải thực đơn hoặc tốc độ đặt hàng sau đăng nhập.

## Các thay đổi đã triển khai

1. `AuthRepositoryImpl` nhận `dagger.Lazy<ApiService>`: không tạo Retrofit/OkHttp
   chỉ để biết người dùng chưa có token. Khi người dùng đăng nhập hoặc xác thực
   tài khoản đã có token, API vẫn được khởi tạo và sử dụng bình thường.
2. `AuthPreference` tạo Gson khi thực sự cần đọc/ghi giỏ hàng.
3. `LoginScreen` trì hoãn CredentialManager đến lúc người dùng bấm đăng nhập Google,
   đồng thời giữ một instance theo context bằng `remember`.
4. `MainScreen` dùng chung HomeViewModel ở phạm vi màn hình chính, tránh tạo thêm
   HomeViewModel trong tab Home/Menu. Thay đổi này không được đo bởi kịch bản đăng xuất.
5. Bật R8/resource shrinking cho release; thêm keep rules cho DTO và model Gson.
6. Thu Baseline/Startup Profiles từ thực thi thật bằng `BaselineProfileRule`.
   Profile generation chạy trên app không minify/optimize; release dùng R8 để rewrite.
   Vì chỉ thu một hành trình khởi động, bộ quy tắc HRF do generator xuất cho startup
   được dùng cho cả Baseline và Startup Profile. Nếu thêm hành trình khác như cuộn
   thực đơn, chỉ hợp nhất quy tắc đó vào Baseline, không đưa vào Startup Profile.
7. ProfileInstaller được đóng gói trong app. Startup Profile được đưa vào đầu vào
   source set `baselineProfiles`, không chỉ lưu một file không được sử dụng.
8. Thêm `ReportDrawn()` tại màn hình đăng nhập: TTFD là thời điểm màn hình này vẽ
   xong để sử dụng. Splash không được tính là màn hình hoàn tất.

Các cờ thực nghiệm phục vụ tách tác động. Mặc định hiện bật lazy, R8, Baseline Profile
và Startup Profile để triển khai đầy đủ đề tài. Tắt riêng Startup Profile bằng
`-PstartupProfile=false` để tạo đối chứng E. Việc áp dụng đã được xác nhận bằng
metadata R8 (`startup: true`) và hash DEX khớp APK; hiệu quả thời gian được đánh giá
riêng, không suy ra từ việc build thành công. Lượt A/E cũ là kết quả trước thay đổi
mặc định này, không được coi là số liệu của cấu hình có cả hai profile.
Không bỏ qua xác thực phiên để tạo số liệu khởi động đẹp hơn.

Khởi tạo lazy chuyển một phần chi phí sang lần dùng đầu tiên (ví dụ bấm đăng nhập
Google hoặc gửi yêu cầu đăng nhập); nó không làm biến mất chi phí đó. Cần đo thêm
độ trễ thao tác đầu tiên nếu mở rộng nghiên cứu sang toàn bộ hành trình sử dụng.
Profile hiện tại chỉ đại diện cho hành trình chưa đăng nhập đã thu thập.

## Ma trận thí nghiệm

| Mã | Khởi tạo | R8 | Profile app đóng gói | Startup Profile | ART compilation |
|---|---|---|---|---|---|
| A | Eager như trước | Tắt | Không | Không | None |
| B | Lazy | Tắt | Không | Không | None |
| C | Lazy | Bật | Không | Không | None |
| D | Lazy | Bật | Có | Không | None |
| E | Lazy | Bật | Có | Không | Partial, yêu cầu profile |
| F | Lazy | Bật | Có | Có | Partial, yêu cầu profile |
| G | Lazy | Tắt | Có | Không | Partial, yêu cầu profile |

- A/B: đánh giá thay đổi khởi tạo; cùng instrumentation TTID/TTFD.
- B/C: đánh giá tối ưu build, không quy hết hiệu quả cho profile.
- D/E: cùng APK; đánh giá ART compilation từ profile. Profile thư viện cũng có
  thể tham gia nên không gọi đây là tác động của riêng quy tắc app.
- E/F: cùng R8 và chế độ ART; thay đổi đầu vào Startup Profile để đánh giá thêm
  tác động bố trí DEX.
- A/F: kết quả tổng hợp của toàn bộ phương án trong kịch bản đã định nghĩa.
- B/G: thêm Baseline Profile và biên dịch theo profile khi không dùng R8; kiểm tra
  phương án ưu tiên khởi động thay vì chỉ tối thiểu hóa APK.

App vẫn có các profile từ thư viện; `appProfiles=false` chỉ loại profile do nhóm
thu thập, không tuyên bố loại mọi profile. `CompilationMode.None` kiểm soát AOT
trong các phép đo tương ứng. Cold start không đồng nghĩa xóa toàn bộ cache thiết bị.

## Chạy một lần toàn bộ quy trình

Mở đúng dự án, sync Gradle. Kết nối đúng một điện thoại Android API 33+ để sinh
profile không cần root (khuyến nghị API 34+ để reset trạng thái biên dịch mà giữ
dữ liệu thuận tiện), mở khóa. Riêng chạy Measure dùng module có minSdk 28.
Đóng các tác vụ nặng, giữ pin/nhiệt độ ổn định. Chờ Android khởi động hoàn tất.

Trong PowerShell tại gốc repo:

```powershell
.\scripts\Run-StartupStudy.ps1
```

Chạy thử trên máy ảo:

```powershell
.\scripts\Run-StartupStudy.ps1 -Emulator
```

Script chạy generator, sao chép profile vào source, chạy bảy phép đo (mặc định
10 lần mỗi phép), lưu kết quả từng cấu hình trước khi lượt sau ghi đè thư mục build,
rồi kiểm tra điều hướng Login → Register → Login trên cấu hình cuối cùng.
Giữ máy chạy; không thao tác/đăng nhập vào bản app `Food App Benchmark` trong lúc đo.
Nếu đã đăng nhập nhầm, xóa dữ liệu riêng package `com.example.jetpackcompose.benchmark`.

Chạy riêng giai đoạn:

```powershell
.\scripts\Run-StartupStudy.ps1 -Mode Generate -Emulator
.\scripts\Run-StartupStudy.ps1 -Mode Measure -Emulator
```

Nếu Windows báo script bị chặn bởi Execution Policy, gọi script trong một tiến
trình PowerShell riêng (không thay đổi chính sách hệ thống):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Run-StartupStudy.ps1 -Emulator
```

Nếu dùng máy ảo ở chế độ headless và ảnh chụp bị đen hoặc thao tác chạm không tới
app, kiểm tra bộ dựng hình. Lượt xác minh trong repo dùng SwiftShader với các cờ
`-gpu swiftshader -no-snapshot-load -no-snapshot-save`. Cần khởi động lại máy ảo
và chạy lại cả bộ so sánh khi đổi renderer; không trộn số liệu hai renderer.
Có thể chọn đồ họa phần mềm trong cấu hình AVD và Cold Boot từ Device Manager.

Có thể truyền `-Serial emulator-5554`, `-Iterations 10`, `-OutputDirectory <duong-dan>`.
Nếu bị gián đoạn, truyền `-Resume` cùng OutputDirectory để dùng lại các cấu hình
đã lưu xong. Chỉ resume khi vẫn giữ cùng mã nguồn, thiết bị và số lần đo; nếu đã sửa
code, bắt đầu một thư mục thí nghiệm mới.
Chỉ bỏ qua lỗi EMULATOR khi truyền rõ `-Emulator`; không bỏ qua lỗi app debug,
thiếu profile, nhiệt độ hoặc pin yếu.

Không cần cài thêm Baseline Profile Gradle plugin: quy trình dùng Macrobenchmark
và source-set API của AGP, script quản lý việc thu/copy file. Tránh việc mỗi lần
build release tự mở máy ảo để sinh profile. Sau khi thay đổi luồng khởi động,
chạy lại Generate/Full để cập nhật các profile đã lưu.

## Đọc và bảo toàn kết quả

Mỗi lượt có thư mục `build/startup-study/<timestamp>/` chứa:

- `environment.json`: thiết bị, thời gian, commit gốc và trạng thái working tree.
- `source-hashes.csv`: SHA-256 của mã nguồn và cấu hình đầu vào tại lúc bắt đầu lượt đo.
- `summary.csv`, `summary.json`: median/min/max/mean, số lần đo và dung lượng APK.
- Thư mục A–G: JSON nguyên gốc, trace mỗi lần, cấu hình thí nghiệm.
- Script hiện tại còn lưu APK và SHA-256 của mỗi cấu hình để đối chiếu bản đo.
- Log build, sinh profile và smoke test.

Để demo bản tối ưu không debug, cài APK đã lưu ở cấu hình được chọn trong kết quả
(ví dụ `E-baseline-aot/app-benchmark.apk`). Nút Run với build variant debug phục vụ
phát triển, không tái hiện cấu hình R8/AOT của phép đo. Macrobenchmark chủ động áp
dụng chế độ ART tương ứng; một lần cài APK thủ công không thay thế phép đo có kiểm soát.

TTID/TTFD lấy từ `StartupTimingMetric`, không lấy thời gian cài APK/build Gradle.
Mở `.perfetto-trace` bằng Android Studio File > Open hoặc Perfetto UI. Xem tiến trình
`com.example.jetpackcompose.benchmark`, main thread, activity startup, các đoạn
class loading và scheduling. Một đoạn dài có thể bao gồm thời gian bị chờ CPU,
không mặc định coi toàn bộ là thời gian CPU thực thi.

Profile binary cần có trong APK ở `assets/dexopt/baseline.prof`. Kiểm tra báo cáo R8
và log merge Startup Profile để xác nhận đầu vào bố trí DEX được tiêu thụ. Một file
profile tồn tại trong source chưa đủ chứng minh nó đã được áp dụng.

Không ghi số liệu chưa chạy. Không loại các lần đầu cao hơn chỉ vì chúng làm kết
quả xấu. Lưu mọi lần; so sánh trung vị, độ phân tán và tái lập nhiều đợt. Nên đổi
thứ tự cấu hình giữa các đợt đo chính thức để hạn chế ảnh hưởng nhiệt độ/cache.
Kết quả máy ảo dùng kiểm tra quy trình; chưa chứng minh tốc độ trên máy thật.

Để xác nhận A/E bằng APK có sẵn và giảm ảnh hưởng build nền, dùng thứ tự ABBA:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Confirm-StartupPair.ps1 -StudyDirectory <thu-muc-ket-qua-A-G>
# Với máy ảo, thêm -Emulator. Với nhiều thiết bị, chỉ định -Serial.
```

Script dừng Gradle daemon, cài test APK đã build, chạy A1/E1/E2/A2 bằng ADB và lấy
raw JSON/trace sau mỗi khối. Dùng cùng thiết bị đã sẵn sàng, giữ nguyên renderer.
Mỗi khối mặc định 5 lần, tổng 10 lần cho mỗi cấu hình. Khi đổi đường dẫn kết quả,
truyền `-OutputDirectory` để lưu một lượt mới, không ghi đè lượt trước. Script hiện
mặc định tạo tên thư mục theo thời điểm, từ chối thư mục có dữ liệu và kiểm tra hash
APK đầu vào. Sau đo, tự tính bảng/biểu đồ bằng `Export-StartupResults.ps1`.

Tài liệu học từ đầu, sơ đồ thực tế và hồ sơ số liệu được đóng gói riêng tại
[android-startup-guide/README.md](android-startup-guide/README.md).

## Giới hạn kiểm thử

Smoke test kiểm tra đăng nhập/đăng ký và điều hướng, không tạo tài khoản hoặc giao
dịch thật. Google client ID trong repo vẫn là placeholder. TTFD cho người đã đăng
nhập cần một kịch bản riêng kiểm soát backend và báo fully-drawn ở màn hình chính.
Không suy ra hiệu quả tiết kiệm pin/RAM nếu chưa đo hai đại lượng đó.

## Nguồn kỹ thuật

- https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
- https://developer.android.com/topic/performance/baselineprofiles/overview
- https://developer.android.com/topic/performance/startupprofiles/dex-layout-optimizations

Đây là tài liệu triển khai và nhật ký thực nghiệm để nhóm tự phân tích, không phải
bản báo cáo học phần đã hoàn thành.
