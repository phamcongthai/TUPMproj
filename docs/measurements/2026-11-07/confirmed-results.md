# Kết quả xác nhận cuối — 07/11/2026

## Phạm vi và quy trình

Máy ảo Medium Phone API 37, Android 17, SwiftShader, Windows. App mở đến màn hình
đăng nhập, chưa có tài khoản đăng nhập. TTID đo hiển thị ban đầu; `ReportDrawn()`
ở LoginScreen báo TTFD. Không dùng thời gian build/cài APK làm thời gian khởi động.

Sau bộ thăm dò A–G, dừng Gradle daemon và dùng APK đã lưu, không build giữa các
phép đo xác nhận. Thứ tự **A1 → E1 → E2 → A2**, mỗi khối 5 cold starts:

- A: tái hiện khởi tạo eager, không R8, `CompilationMode.None`.
- E: lazy + R8 + Baseline Profile, `Partial(Require, warmupIterations=0)`.
- Cùng package benchmark, cùng màn hình, instrumentation và máy ảo.
- Gộp toàn bộ 10 giá trị mỗi bản rồi lấy trung vị; không loại các lần chậm.

Thứ tự đối xứng hạn chế ảnh hưởng thứ tự/tải máy biến đổi; chưa loại bỏ mọi nhiễu.
Đã xác nhận ảnh hiển thị và thao tác chạm trên SwiftShader hoạt động. Lượt renderer
cũ bị ảnh đen được giữ để chẩn đoán, không dùng làm kết luận tăng tốc.

## Kết quả xác nhận

| Chỉ số | A — đối chứng | E — bản được chọn | Thay đổi |
|---|---:|---:|---:|
| TTID, trung vị | 1617.202 ms | 1390.739 ms | Giảm 14.00% |
| TTFD, trung vị | 2293.035 ms | 1698.688 ms | Giảm 25.92% |
| APK | 16096962 byte | 2746797 byte | Giảm 82.94% |

Đây là tác động tổng hợp của E trong lượt xác nhận này, không phải mức tăng tốc
riêng của Baseline Profile hoặc lazy. Dải thời gian còn chồng lấn; chưa tính khoảng
tin cậy hay chứng minh ý nghĩa thống kê. Cần đo nhiều đợt trên điện thoại thật.

Raw JSON/CSV được sao lưu trong `confirmed-pair/` cạnh tài liệu này. Trace và log
đầy đủ: `build/startup-study/pair-confirmation/`. APK E được copy nguyên trạng tới
`build/artifacts/food-app-optimized.apk`.

## Bộ thăm dò trên SwiftShader

Mỗi cấu hình 5 lần; trung vị ms. Build được thực hiện giữa các cấu hình nên tải
máy có thể ảnh hưởng. Không dùng chênh lệch từng hàng làm ước lượng nhân quả chính xác.

| Cấu hình | TTID | TTFD |
|---|---:|---:|
| A — eager, không R8, không AOT | 1129.663 | 1803.787 |
| B — lazy, không R8, không AOT | 1249.510 | 1796.062 |
| C — lazy + R8, không AOT | 2615.297 | 3906.064 |
| D — thêm Baseline Profile, không AOT | 1659.755 | 2270.084 |
| E — áp dụng Baseline Profile | 1722.365 | 2129.723 |
| F — thêm Startup Profile | 1848.158 | 2145.901 |
| G — Baseline Profile, không R8 | 3573.528 | 4436.250 |

Kết quả dao động và có cấu hình chậm hơn đối chứng. Vì vậy đã thực hiện lượt A/E
riêng thay vì chọn các giá trị đẹp từ bảng này. Bộ thăm dò được giữ trong
`swiftshader-study/` và thư mục build tương ứng.

## Cấu hình mặc định và đánh đổi

Chọn E làm cấu hình release/benchmark mặc định: lazy, R8/resource shrinking và
Baseline Profile. E đã được kiểm tra bằng lượt A/E riêng và giảm dung lượng APK.

Startup Profile đã sinh từ thực thi, merge vào R8, build và đo ở F. Lượt thăm dò
chưa cho thấy lợi ích bổ sung, còn APK tăng 98362 byte, nên giữ opt-in bằng
`-PstartupProfile=true`. Không kết luận nó luôn làm chậm app; cần đo trên máy thật.

Profile hiện thu hành trình chưa đăng nhập. Lazy chuyển chi phí sang lần sử dụng
đầu tiên; cần đo riêng thao tác đăng nhập nếu mở rộng nghiên cứu. Chưa đo RAM,
pin, tải thực đơn, đặt hàng hoặc thanh toán thật.

## Kiểm tra đã hoàn tất

- Bảy cấu hình, 35 cold starts ở bộ SwiftShader.
- Xác nhận ABBA: 20 cold starts; đủ 10 lần mỗi APK, bốn instrumentation tests pass.
- Login → Register → Login pass trên bản minified F và bản không minify G.
- APK E bàn giao đã được cài, áp dụng profile, biên dịch speed-profile và kiểm tra
  Login → Register → Login trực tiếp bằng ADB thành công.
- Binary Baseline Profile có trong APK; R8 rewrite vẫn có quy tắc MainActivity.
- Startup Profile được tiêu thụ ở F; APK và SHA-256 được lưu theo cấu hình.

SHA-256 mã nguồn và cấu hình tại lúc bàn giao: `source-hashes-at-delivery.csv`.

Quy trình tái lập: [startup-study.md](../../startup-study.md).
Đây là nhật ký thực nghiệm để nhóm tự nghiên cứu, không phải báo cáo học phần
hoàn chỉnh hay kết quả bảo đảm cho mọi thiết bị.
