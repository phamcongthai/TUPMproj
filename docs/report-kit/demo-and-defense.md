# Demo 7 phút và câu hỏi bảo vệ

**Mục tiêu demo:** chỉ ra hai profile đã được áp dụng và giải thích kết quả đối chứng; không trình diễn toàn bộ nghiệp vụ đặt đồ ăn.

Nguồn số liệu: [báo cáo cập nhật](../physical-profile-results.md). Không dùng bảng A/E máy ảo cũ để quy công riêng cho profile.

## Chuẩn bị

Mở app benchmark ở Login, nguồn `app/build.gradle.kts`, hai thư mục profile, JSON kiểm tra DEX và bảng/biểu đồ của phiên xác nhận Startup. Chuẩn bị APK từ trước; build R8 và đo 20 lượt mất nhiều phút, không đưa vào kịch bản 7 phút.

APK hiện tại trên máy thực nghiệm: `build/artifacts/food-app-initial-display-startup-profile.apk`. Tệp này thuộc thư mục build nên không nằm trong Git. Khi clone mới, mở thư mục gốc có `settings.gradle.kts`, cài SDK theo Gradle, rồi build bản benchmark:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleBenchmark :macrobenchmark:assembleBenchmark --max-workers=2
```

APK tạo ở `app/build/outputs/apk/benchmark/app-benchmark.apk`; bật đủ profile theo cấu hình mặc định. Bản debug chạy bằng Studio không thay thế bản benchmark khi đánh giá tối ưu này. Cài và mở app không tự chứng minh đã AOT như phép đo; xem log chuẩn bị ART và hướng dẫn trong nhật ký.

## Trình tự trình bày

| Thời gian | Thao tác | Điều phải nói |
|---|---|---|
| 0:00–0:45 | Mở Login và nêu đề tài | Phạm vi chưa đăng nhập → Login; TTID khác TTFD |
| 0:45–2:00 | Chỉ hai profile và Gradle | Baseline cho ART biên dịch trước; Startup cho R8 bố trí DEX; hai cơ chế khác nhau |
| 2:00–3:00 | Mở metadata/kiểm tra DEX | DEX đầu startup, MainActivity ở trong, checksum khớp; Baseline được đóng binary |
| 3:00–3:30 | Login → Register → Login | Kiểm tra điều hướng sau build tối ưu, không gửi tài khoản/giao dịch |
| 3:30–4:30 | Mở bảng Baseline D/E | Cùng APK, khác ART; TTID giảm 12,95%, TTFD giảm 22,86% trong phiên này |
| 4:30–6:00 | Mở bảng hai phiên Startup và biểu đồ xác nhận | E/F cùng Baseline, chỉ thêm Startup; xác nhận đảo thứ tự giảm 7,66%/11,05%; trình bày cả số thăm dò và dao động |
| 6:00–7:00 | Mở CSV, audit, giới hạn | Dữ liệu gốc được giữ; một thiết bị; không cộng phần trăm; không khẳng định thống kê hoặc mọi Android |

Bằng chứng DEX: `docs/measurements/2026-11-07/physical-profiles/initial-display/startup-verification.json`.

Dữ liệu xác nhận: `docs/measurements/2026-11-07/physical-profiles/initial-display/confirmation/` (results.md, CSV, JSON, audit, PNG). Đối chứng Baseline: `docs/measurements/2026-11-07/physical-profiles/phone-captured-idle/`.

Có thể tạo lại bảng/biểu đồ từ thống kê và CSV đã lưu, không phát sinh số đo mới:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Export-ProfileEffects.ps1 -InputDirectory docs/measurements/2026-11-07/physical-profiles/initial-display/confirmation -OutputDirectory build/demo-confirmation -ProfilePairOnly
```

Đây là xuất lại dữ liệu, không phải benchmark trực tiếp hoặc chạy lại đầy đủ audit. Audit đầy đủ yêu cầu các Perfetto trace gốc; trace/APK trong build không được commit. Muốn có bộ trace mới trên máy khác phải thực hiện phép đo mới theo nhật ký và giữ thành phiên riêng.

## Câu hỏi bảo vệ

**Đóng góp của nhóm là gì?** Nghiên cứu cơ chế có sẵn, tích hợp trên app gốc, xác định hành trình, thu profile, xây dựng đối chứng, đo trên điện thoại và phân tích kết quả. Không phát minh thuật toán ART/R8.

**Làm sao tách hai kỹ thuật?** Baseline: D/E cùng APK, từ verify sang Baseline + speed-profile. Startup: E/F cùng Baseline và mã app, bật thêm Startup ở F. Lazy/R8 giữ nguyên trong từng phép so sánh.

**Tại sao không lấy thời gian Install successfully?** Đó là cài đặt, không phải khởi động. Phép đo dùng StartupTimingMetric, TTID và mốc ReportDrawn của Login.

**Tại sao Startup trước không cải thiện?** Bộ cũ đã được áp dụng nhưng chưa cho lợi ích thời gian. Nhóm thử thu ngay sau hiển thị ban đầu thay cho chờ Login và công việc muộn; bộ mới cho cải thiện ở hai phiên. Chưa cô lập riêng thời điểm thu là nguyên nhân duy nhất vì Baseline hợp nhất và bộ quy tắc cũng khác giữa các cặp build.

**Có phải lúc nào Startup cũng giảm 20,49%?** Không. Đó là phiên thăm dò; phiên đảo thứ tự giảm TTID 7,66%, TTFD 11,05%. Giữ cả hai để thể hiện mức dao động, không chỉ chọn số lớn.

**Có cộng 12,95% với 7,66% được không?** Không. Đó là hai phép so sánh/phiên khác nhau; chưa đo một hiệu quả tổng hợp tương ứng bằng cách cộng chúng.

**APK nhỏ hơn hay RAM/pin tốt hơn chưa?** Không dùng gói giảm APK cũ để chứng minh profile. Với cặp Startup mới, APK F lớn hơn E. Chưa đo RAM/pin.

**Đã kiểm tra toàn bộ app chưa?** Mới xác nhận startup và Login → Register → Login. Chưa kiểm thử end-to-end tài khoản, Google, menu, đặt món hoặc thanh toán.

**Tại sao audit PASS chưa có nghĩa mọi máy đều nhanh hơn?** Audit kiểm tra số liệu, APK, chuẩn bị ART và trace. Khả năng khái quát/ý nghĩa thống kê cần thiết kế và dữ liệu bổ sung.

**Bản README học ghi khác thì dùng số nào?** Hai README học được giữ nguyên theo yêu cầu đọc/feedback. Kết luận hiện tại lấy từ `docs/physical-profile-results.md` và dữ liệu được liên kết tại đó.
