# Chốt đề tài và bộ bàn giao

**Mục tiêu:** áp dụng Baseline Profiles và Startup Profiles để giảm thời gian khởi động Food App; đo đối chứng để đánh giá tác động riêng của từng kỹ thuật.

**Phạm vi:** mở ứng dụng khi chưa đăng nhập đến màn hình Login. Nghiên cứu và ứng dụng kỹ thuật Android có sẵn; không đề xuất thuật toán mới.

## Kết quả dùng trong báo cáo

| Phép so sánh | TTID giảm | TTFD giảm | Dữ liệu |
|---|---:|---:|---|
| Chưa AOT → Baseline AOT, cùng APK | 12,95% | 22,86% | Phiên D/E/F trên điện thoại, 9 lượt/cấu hình |
| Chỉ Baseline → thêm Startup, xác nhận đảo thứ tự | 7,66% | 11,05% | Phiên F–E–E–F, 10 lượt/cấu hình |

Hai phép so sánh thuộc hai phiên/bộ profile khác nhau: **không cộng phần trăm**, không dùng E của phiên này thay E của phiên kia. Trong mỗi phép so sánh, mã app, lazy initialization và R8 được giữ giống nhau. Startup còn có phiên thăm dò giảm 20,49%/18,52%; dùng số của phiên xác nhận khi trình bày kết quả xác nhận.

**Câu kết luận có thể dùng:** Trong điều kiện thực nghiệm trên Redmi 23124RN87C, Android 13, Baseline cho cải thiện thời gian khởi động quan sát được; bộ Startup tập trung vào hiển thị ban đầu tiếp tục cho cải thiện ở hai phiên riêng, kể cả khi đảo thứ tự chạy. Chưa thiết lập ý nghĩa thống kê hoặc khái quát sang thiết bị khác.

## Đã hoàn thành

- Tích hợp Baseline/Startup vào bản benchmark kế thừa release, không debug; kiểm tra R8 thực sự bố trí DEX và Baseline được cài, ART biên dịch `speed-profile`.
- Sinh profile từ chạy app thật, giữ Baseline cho Login và thu Startup ở hiển thị ban đầu; không thêm quy tắc giả để lấy kết quả.
- Đo trên điện thoại, giữ JSON/CSV/log gốc, biểu đồ, checksum APK và báo cáo kiểm tra. Giữ cả kết quả chưa đạt và phiên bị nhiễu, không trộn vào phiên xác nhận.
- Kiểm tra Login → Register → Login thành công; chưa kiểm thử end-to-end đặt món/thanh toán hoặc chứng minh tối ưu RAM/pin.

## Đọc và trình bày theo thứ tự

1. [Báo cáo kết quả chuẩn](physical-profile-results.md): nguồn số liệu cập nhật.
2. [Bằng chứng triển khai](startup-profile-applied.md): áp dụng ở đâu và kiểm tra thế nào.
3. [Khung báo cáo 25–30 trang](report-kit/report-outline.md): cấu trúc để nhóm tự viết.
4. [Kịch bản demo và trả lời bảo vệ](report-kit/demo-and-defense.md).
5. [Nhật ký thực nghiệm](profile-refinement-notes.md): chi tiết, lệnh tái lập và các kết quả trước.

Mã trọng tâm: `app/build.gradle.kts`, `app/src/main/profiles/`, `macrobenchmark/src/main/java/com/example/foodapp/macrobenchmark/StartupBenchmark.kt`, `scripts/Capture-PhoneStartupProfile.ps1`, `scripts/Confirm-ProfileEffects.ps1`.

**Trạng thái:** phần triển khai và đo kiểm đã xong. Phần nhóm còn thực hiện là viết báo cáo theo cách hiểu của mình, bổ sung thông tin thành viên và tập trình bày. Hai README hướng dẫn học được giữ nguyên theo yêu cầu trước; số liệu kết luận lấy từ các liên kết cập nhật ở trên.
