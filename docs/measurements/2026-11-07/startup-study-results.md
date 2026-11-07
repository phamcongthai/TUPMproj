# Nhật ký chẩn đoán renderer — không dùng số liệu này để kết luận tăng tốc

> Lượt chẩn đoán ban đầu: sau đó phát hiện lỗi renderer headless làm ảnh màn hình
> bị đen và input không tới app. Không dùng các mức tăng tốc bên dưới làm kết luận
> thực nghiệm chính thức. Giữ dữ liệu để truy vết; xem [kết quả xác nhận](confirmed-results.md)
> trên SwiftShader và lượt đối chứng ABBA.

## Điều kiện và phạm vi

- Máy ảo Medium Phone API 37, Android 17, trên máy phát triển Windows.
- App benchmark không debug, package riêng `com.example.jetpackcompose.benchmark`.
- Người dùng chưa đăng nhập, mở app đến màn hình đăng nhập. Không gọi backend.
- Mỗi cấu hình: 10 lần cold start; `StartupTimingMetric`, có `ReportDrawn()` cho TTFD.
- A là cấu hình tái hiện khởi tạo eager trong cùng cây mã và cùng instrumentation;
  không so trực tiếp với phép đo ADB bản debug trước đây.
- Toàn bộ dữ liệu được giữ, không loại các lần đầu chậm hơn.

## Số liệu trung vị

Đơn vị thời gian: ms. Dung lượng APK là byte, chưa phải dung lượng cài đặt.

| Cấu hình | TTID | TTFD | APK |
|---|---:|---:|---:|
| A — Eager, không R8, không AOT | 1562.092 | 2234.871 | 16096962 |
| B — Lazy, không R8, không AOT | 1273.504 | 1902.470 | 16096962 |
| C — Lazy + R8, không AOT | 789.428 | 1090.649 | 2746797 |
| D — Thêm Baseline Profile, không AOT | 762.120 | 1045.929 | 2746797 |
| E — Áp dụng Baseline Profile bằng Partial/Require | 805.298 | 867.567 | 2746797 |
| F — E + Startup Profile | 882.254 | 882.254 | 2845159 |

So sánh A với E trong lượt này: TTID giảm 48.45%, TTFD giảm 61.18%, APK giảm 82.94%.
Đây là kết quả tổng hợp của lazy + R8 + profile, không phải mức tăng tốc riêng của
Baseline Profile. Các tỷ lệ được tính từ trung vị tương ứng của từng cấu hình.

## Diễn giải và lựa chọn mặc định

- R8 tạo ra phần giảm dung lượng lớn; keep rules giữ DTO/model Gson phục vụ API và
  giỏ hàng. Không đánh đồng giảm APK với giảm RAM hoặc điện năng.
- D/E là cùng APK, khác chế độ ART compilation. TTFD tốt hơn khi áp dụng profile;
  TTID trong lượt này tăng nhẹ. Chưa thể kết luận mọi chỉ số đều tốt hơn.
- E/F kiểm tra thêm Startup Profile. Chưa thấy lợi ích bổ sung; APK tăng 98362 byte.
  Startup Profile đã triển khai và đo, nhưng mặc định tắt (`startupProfile=false`).
  Có thể bật lại bằng `-PstartupProfile=true` để trình diễn và đo trên thiết bị thật.
- B có độ dao động cao. Trong giai đoạn B có một truy vấn phân tích trace chạy trên
  cùng máy ảo; xem đây là thăm dò, không dùng A/B làm ước lượng nhân quả chính xác.
  Cần đo lại B trong điều kiện không có tác vụ phân tích nền khi viết báo cáo chính thức.

Mặc định chọn E: lazy + R8 + Baseline Profile. Quyết định này dựa trên lượt đo máy
ảo hiện có; cần đo lại nhiều đợt trên điện thoại thật trước khi khái quát hóa.

## Bằng chứng kỹ thuật

- Sinh profile từ thực thi thật: 17749 quy tắc, 502 quy tắc liên quan package app.
- Input Startup Profile được merge vào `merged_startup_profile` khi bật cấu hình F.
- R8 rewrite profile và vẫn giữ quy tắc MainActivity của app ở đầu ra.
- APK có binary `assets/dexopt/baseline.prof`, nhỏ hơn giới hạn 1.5 MB.
- Các bài đo E/F dùng `BaselineProfileMode.Require`, không tự bỏ qua profile thiếu.
- Mỗi lần đo có trace riêng; SHA-256 của APK được lưu theo cấu hình.

Một trace minh họa của A (iteration 005) có `bindApplication` khoảng 465.866 ms,
`OpenDexFilesFromOat` khoảng 237.195 ms và `Recomposer:recompose` khoảng 466.595 ms.
Đây là thời lượng inclusive của một trace, các đoạn có thể lồng nhau và gồm thời
gian chờ CPU; không cộng chúng để suy ra tổng CPU. Trace Processor còn báo trường
extension chưa nhận diện trên Android 17, nên không dùng truy vấn này thay số liệu
startup chính thức từ Macrobenchmark.

## Giới hạn và bước sử dụng cho học phần

Đây là thực nghiệm thăm dò trên một máy ảo, một thứ tự chạy A–F, chưa kiểm soát
hoàn toàn tải/nhiệt độ máy chủ. Không có khoảng tin cậy hay bằng chứng ý nghĩa
thống kê từ nhiều đợt đo. Không kết luận Startup Profile luôn làm chậm app.

Chưa đo người đã đăng nhập, tải thực đơn, thao tác đăng nhập đầu tiên sau lazy,
RAM hay pin. Không tạo tài khoản, gửi đơn hoặc thanh toán thật trong kiểm thử.

Nhóm có thể dùng mã triển khai, raw JSON và quy trình để thực nghiệm lại trên
điện thoại thật, tự phân tích và viết báo cáo. Không dùng các con số này như kết
quả đo trên máy thật hoặc lời cam kết tăng tốc cho mọi thiết bị.

Quy trình tái lập: [startup-study.md](../../startup-study.md).
