# Kiểm chứng Baseline Profiles và Startup Profiles trên Food App

**Đề tài:** Nghiên cứu và thực nghiệm tối ưu hiệu năng khởi động ứng dụng Android bằng Baseline Profiles và Startup Profiles trên ứng dụng đặt đồ ăn.

**Kết luận hiện tại:** đã có cải thiện quan sát riêng cho Baseline và Startup Profile trong các phép đo có đối chứng. Bộ Startup mới cho thời gian thấp hơn ở **hai phiên riêng**, kể cả phiên đảo thứ tự. Đây là kết quả trên một điện thoại, chưa thiết lập ý nghĩa thống kê hoặc bảo đảm cho mọi thiết bị.

## Startup Profile: kết quả mới nhất

Redmi 23124RN87C, Android 13, ARM64; mở app chưa đăng nhập đến Login. Người dùng xác nhận để máy tự chạy. Mỗi phiên 20 cold start, 10 lượt/cấu hình; giữ mọi lượt. TTID là thời gian hiển thị ban đầu, TTFD đến khi Login gọi `ReportDrawn`.

| Phiên | Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---|---:|---:|
| Thăm dò E–F–F–E | E: chỉ Baseline | 895,963 | 1.014,992 |
| Thăm dò E–F–F–E | F: Baseline + Startup | 712,409 | 827,044 |
| Xác nhận F–E–E–F | E: chỉ Baseline | 909,420 | 1.064,026 |
| Xác nhận F–E–E–F | F: Baseline + Startup | 839,737 | 946,451 |

| So sánh E→F trong từng phiên | TTID giảm | TTFD giảm |
|---|---:|---:|
| Thăm dò, 20 lượt | **20,49%** | **18,52%** |
| Xác nhận đảo thứ tự, 20 lượt | **7,66%** | **11,05%** |

**Cách diễn giải:** lợi ích mô tả đã lặp lại, nhưng mức giảm dao động. Dùng mức của phiên xác nhận khi trình bày kết quả xác nhận; không quảng bá 20,49% là mức luôn đạt được. Không gộp 40 lượt để tạo một mức phần trăm chung.

**Kiểm soát:** E/F cùng mã app, lazy initialization, R8/resource shrinking và Baseline đầu vào; chỉ bật thêm Startup Profile ở F. Hai phiên dùng đúng cùng APK, runner, mã benchmark và đầu vào profile. Phiên xác nhận đảo điều kiện chạy trước; driver chỉ thêm tùy chọn thứ tự. Mỗi khối cài mới riêng package benchmark, cài Baseline rồi biên dịch `speed-profile`; benchmark tắt bước tự chuẩn bị compilation để giữ trạng thái đã ghi nhận. Không lấy hiệu quả thay đổi lazy hoặc R8 để quy công cho profile.

[Thăm dò: bảng, biểu đồ, CSV, JSON](measurements/2026-11-07/physical-profiles/initial-display/exploratory/results.md) · [Xác nhận: bảng và biểu đồ](measurements/2026-11-07/physical-profiles/initial-display/confirmation/results.md) · [Audit xác nhận: PASS](measurements/2026-11-07/physical-profiles/initial-display/confirmation/audit.json) · [Kiểm tra đồng nhất hai phiên: PASS](measurements/2026-11-07/physical-profiles/initial-display/cross-study-check.json).

## Đã điều chỉnh gì?

Bộ trước chờ 5 giây và kiểm tra UI trước khi lưu profile. Bộ mới lưu ngay sau khi `am start -W` báo hiển thị ban đầu, tránh đưa thêm tác vụ muộn vào hành trình Startup. ART vẫn cần khoảng chờ 1 giây để lưu bất đồng bộ; không coi đây là ranh giới chính xác tuyệt đối theo mili giây.

Sau 11 vòng thu, ba tập quy tắc cuối giống nhau. Startup mới có **17.249 dòng** so với 18.037 trước. Baseline giữ hành trình Login rộng và hợp nhất quy tắc mới thành **18.047 quy tắc riêng biệt**, dùng giống nhau cho E/F. Không thêm phương thức hoặc cờ giả. R8 xác nhận DEX đầu startup, chứa MainActivity, SHA-256 DEX khớp APK; binary Baseline có trong APK và ProfileInstaller cài thành công.

DEX đầu E/F lần lượt 3.815.796 / 1.584.720 byte; APK 2.746.797 / 2.845.159 byte. Như vậy bố trí DEX thay đổi và APK tăng kích thước, không phải mọi chỉ số đều giảm.

Không khẳng định riêng thời điểm thu là nguyên nhân duy nhất khiến kết quả khác các phiên trước: bộ quy tắc và Baseline hợp nhất cũng khác. Bằng chứng hiệu quả Startup ở đây là đối chứng E/F trong từng phiên mới.

[Kế hoạch ghi trước đo](measurements/2026-11-07/physical-profiles/initial-display/plan.txt) · [Kế hoạch xác nhận](measurements/2026-11-07/physical-profiles/initial-display/confirmation-plan.txt) · [Nguồn profile](measurements/2026-11-07/physical-profiles/initial-display/capture/capture.json) · [Bằng chứng DEX](measurements/2026-11-07/physical-profiles/initial-display/startup-verification.json).

## Baseline Profile: phép đối chứng riêng trước đó

D/E dùng chính một APK, thay trạng thái ART từ `verify` sang cài Baseline và `speed-profile`. Phiên 27 lượt trước, 9 lượt/cấu hình:

| Cấu hình | TTID trung vị (ms) | TTFD trung vị (ms) |
|---|---:|---:|
| D: chưa AOT cho app | 1.068,375 | 1.380,999 |
| E: biên dịch theo Baseline | 930,032 | 1.065,297 |

TTID giảm **12,95%**, TTFD giảm **22,86%**. Baseline đóng gói có cả quy tắc app và thư viện. Đây là phiên/bộ profile trước, không dùng E của bảng này làm đối chứng cho Startup mới và không cộng hai mức phần trăm.

[Dữ liệu Baseline và giới hạn đầy đủ](measurements/2026-11-07/physical-profiles/phone-captured-idle/results.md).

## Giữ các kết quả chưa đạt để đối chiếu

Startup trước đây chưa cho lợi ích: bộ emulator API 37 tăng trung vị 23,06%/11,46%; biến thể lọc cờ S tăng 9,67%/10,12%; bộ API 33 chờ Login trong phiên để máy tự chạy tăng 11,90%/10,76%. Các phiên này giữ riêng, không bị thay thế bằng số mới. Phiên người dùng có dùng điện thoại được đánh dấu bị nhiễu và không dùng kết luận hiệu quả.

[Profile emulator](measurements/2026-11-07/physical-profiles/resumed/results.md) · [Biến thể S](measurements/2026-11-07/physical-profiles/startup-s-phase/results.md) · [API 33 chờ Login](measurements/2026-11-07/physical-profiles/phone-captured-idle/results.md) · [Phiên bị nhiễu](measurements/2026-11-07/physical-profiles/phone-captured/results.md).

Tổng 141 lượt được ghi nhận trong sáu phiên đầy đủ, gồm 27 lượt bị nhiễu; không gộp để tính hiệu quả. 21 lượt trước lần tạm dừng giữ riêng. Audit kiểm tra dữ liệu và chuẩn bị ART, không tự chứng minh ý nghĩa thống kê. Một thiết bị, số lượt nhỏ, dao động lớn; kết quả chỉ áp dụng cho hành trình và điều kiện đã đo.

APK hiện tại: `build/artifacts/food-app-initial-display-startup-profile.apk`, package `com.example.jetpackcompose.benchmark`. Bộ chứng cứ: `build/artifacts/physical-profile-evidence.zip`. Traces gốc của hai phiên mới: `build/startup-study/physical-initial-display-20261107/measurement` và `confirmation`.

Theo [Android: khác biệt hai profile và phạm vi hiển thị ban đầu](https://developer.android.com/topic/performance/baselineprofiles/difference-baseline-startup), [xác nhận DEX](https://developer.android.com/topic/performance/baselineprofiles/confirm-startup-profiles). [Nhật ký thực nghiệm](profile-refinement-notes.md). Hai file README của tài liệu học vẫn giữ nguyên theo yêu cầu.
