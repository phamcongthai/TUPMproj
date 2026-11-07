# Kết quả áp dụng Baseline Profile và Startup Profile

**Triển khai đã hoàn tất:** bản F bật cả hai profile trong release/benchmark. Metadata R8 đánh dấu `classes.dex` là `startup: true`, bật tối ưu bố trí DEX; SHA-256 của hai DEX khớp chính xác APK. Baseline Profile dạng binary có trong APK. Xem [bằng chứng kiểm tra](startup-verification.json) và [metadata R8 gốc](r8-metadata.json).

APK F: `build/artifacts/food-app-with-startup-profile.apk`, **2.845.159 byte**.

## Lượt xác nhận riêng tác động Startup Profile

API 37 SwiftShader, chưa đăng nhập, cùng lazy/R8/Baseline AOT. E tắt Startup Profile, F bật. Dùng APK đã lưu, dừng Gradle, không build xen giữa. Thứ tự **E1 → F1 → F2 → E2**, 5 cold starts/khối, tổng **10 lần mỗi cấu hình**, không loại lần chậm.

| Chỉ số trung vị | E: chưa thêm Startup | F: đã thêm Startup | Thay đổi |
|---|---:|---:|---:|
| TTID | 2481,769 ms | 2561,546 ms | Tăng 3,21% |
| TTFD | 3073,579 ms | 3186,128 ms | Tăng 3,66% |
| APK | 2.746.797 byte | 2.845.159 byte | Tăng 98.362 byte |

**Kết luận:** Startup Profile đã được áp dụng đúng, nhưng lượt xác nhận riêng chưa chứng minh tăng tốc. Thời gian trung vị cao hơn một ít, dải đo vẫn chồng lấn; chưa có kiểm định để quy chênh lệch này hoàn toàn cho kỹ thuật hoặc kết luận kỹ thuật luôn làm chậm. Không ghi Startup Profile giảm thời gian trong báo cáo dựa trên lượt này.

[Toàn bộ dữ liệu E/F và bảng tính](ef-pair/results.md) · [CSV từng lần](ef-pair/raw-runs.csv) · [TTID](ef-pair/TTID-profiles.png) · [TTFD](ef-pair/TTFD-profiles.png).

## Lượt A/D/E/F trước đó: giữ để đối chiếu, có nhiễu theo thời điểm

40 cold starts theo thứ tự **A1-D1-E1-F1-F2-E2-D2-A2**. Các khối cuối chậm lên rõ:

| Khối | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---:|---:|
| A1 | 1219,422 | 2008,898 |
| A2 | 3000,726 | 4530,205 |

A2 chậm hơn A1 rõ rệt. Do biến động giữa các khối, **không dùng mức giảm E/F 41,88% hoặc A/F 63,83% của lượt tổng hợp để khẳng định hiệu quả nhân quả**. Lượt E/F riêng ở trên được chạy bổ sung để kiểm tra độ ổn định, không gộp hai phiên thành một mẫu chung.

[Dữ liệu và thống kê A/D/E/F](matrix/results.md) · [CSV](matrix/raw-runs.csv) · [Biểu đồ TTFD](matrix/TTFD-profiles.png).

D/E dùng cùng APK, thay None thành Partial/Require để đánh giá compilation theo profile; kết quả tổng hợp TTFD thấp hơn 13,61% nhưng cũng chịu nhiễu theo khối. Profile thư viện có thể tham gia, nên chưa kết luận phần trăm riêng của profile app.

## Kiểm tra chức năng và giới hạn

- 60 cold starts, 12 bài instrumentation đo khởi động pass.
- APK F đã kiểm tra Login → Register → Login, bài smoke test pass. Log: `F-smoke.log`.
- Đã cài profile và yêu cầu ART biên dịch `speed-profile` cho bản F phục vụ demo.
- Chưa đo trên máy thật; chưa kiểm thử thành công đăng nhập, tải thực đơn, đặt hàng hoặc thanh toán.
- Mặc định bật F để triển khai/demo đủ hai kỹ thuật của đề tài; không coi đây là cam kết tốc độ tốt hơn E.

Trace đầy đủ và APK A/E/F nằm trong `build/startup-study/startup-confirmation-20261107/`. Các JSON, log và bảng rút gọn được lưu tại đây. README hướng dẫn học được giữ nguyên theo yêu cầu trong lúc người dùng đọc và góp ý.
