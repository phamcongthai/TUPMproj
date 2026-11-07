# Lượt kiểm tra Macrobenchmark đầu tiên — 07/11/2026

Kết quả cuối sau triển khai: [confirmed-results.md](confirmed-results.md).
Tài liệu dưới đây ghi lại bước kiểm tra ban đầu.

Đây là kiểm tra quy trình trên máy ảo, chưa phải kết quả tối ưu hoặc kết quả
đại diện cho điện thoại thật.

- Thiết bị: Medium Phone API 37.0, Android 17, emulator chạy nền trên máy phát triển.
- App: `com.example.jetpackcompose.benchmark`, bản benchmark không debug,
  kế thừa release hiện tại, tối ưu build vẫn tắt.
- Bài đo: `StartupBenchmark.coldStartupNoCompilation`, 10 lần,
  `StartupMode.COLD`, `CompilationMode.None()`.
- Kịch bản: chưa đăng nhập; chờ màn hình đăng nhập để xác nhận đúng kịch bản.
- Chỉ số: TTID (`timeToInitialDisplayMs`); chưa đo TTFD.
- Chỉ bỏ qua cảnh báo `EMULATOR`; bài test kết thúc thành công.

| Thống kê | Thời gian (ms) |
|---|---:|
| Trung vị | 1456.125 |
| Nhỏ nhất | 1275.123 |
| Lớn nhất | 2994.038 |

Giữ toàn bộ 10 lần đo trong JSON đi kèm, bao gồm các lần đầu cao hơn.
Chưa xác định nguyên nhân dao động. Không suy ra mức cải thiện khi so với ADB debug.
JSON bao gồm thông tin môi trường mà thư viện benchmark thu thập.

Trace và báo cáo HTML nằm trong thư mục build cục bộ (không đưa vào Git):

- `macrobenchmark/build/outputs/connected_android_test_additional_output/`
- `macrobenchmark/build/reports/androidTests/connected/benchmark/index.html`

Khi đo tiếp, sao lưu trace ra thư mục riêng trước vì lượt chạy mới có thể ghi đè
kết quả trong build. Đo chính thức cần thiết bị thật và nhiều lượt thực nghiệm
trong điều kiện kiểm soát, với cùng cấu hình build giữa các phương án.
