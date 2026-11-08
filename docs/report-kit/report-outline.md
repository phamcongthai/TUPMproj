# Khung báo cáo: 27 trang nội dung

**Tên đề tài:** Nghiên cứu và thực nghiệm tối ưu hiệu năng khởi động ứng dụng Android bằng Baseline Profiles và Startup Profiles trên ứng dụng đặt đồ ăn.

**Mục tiêu:** triển khai hai kỹ thuật, đo tác động riêng và thảo luận hiệu quả/giới hạn trên hành trình mở app chưa đăng nhập đến Login. Không chuyển trọng tâm sang lazy initialization, giảm APK, bộ nhớ, pin hoặc bảo mật.

Nguồn số liệu chuẩn: [kết quả cập nhật](../physical-profile-results.md). [Bản chốt bàn giao](../project-handoff.md).

| Chương | Trang | Nội dung phải giải thích | Bằng chứng |
|---|---:|---|---|
| 1. Bài toán và phạm vi | 2 | Vì sao đo startup; giới hạn Login, câu hỏi nghiên cứu | Mục tiêu và phạm vi trong bản chốt |
| 2. Cơ sở lý thuyết | 6 | Kotlin → DEX/APK; ART, JIT/AOT; cold start; TTID/TTFD; khác biệt hai profile | Tài liệu Android chính thức; sơ đồ trong guide học |
| 3. Phân tích ứng dụng và đường khởi động | 3 | MainActivity → Compose/Login; DI; vị trí ReportDrawn; xác định mã cần khi hiển thị | MainActivity, LoginScreen, source app |
| 4. Triển khai hai profile | 5 | Thu/sinh profile; Baseline rộng cho Login; Startup cho hiển thị ban đầu; R8 và ProfileInstaller | app/build.gradle.kts, profile nguồn, capture script, kiểm tra DEX |
| 5. Thiết kế thí nghiệm | 4 | D/E cùng APK khác ART; E/F cùng Baseline khác Startup; điện thoại; thứ tự chạy; biến nhiễu | methodology.json, điều kiện đo, log ART, kế hoạch ghi trước đo |
| 6. Kết quả và thảo luận | 5 | Bảng trước/sau; hai phiên Startup; phân bố, APK, kết quả chưa đạt; giới hạn | CSV/JSON, biểu đồ, audit, checksum |
| 7. Kết luận và hướng mở rộng | 2 | Đã quan sát gì; không khái quát; mở rộng thiết bị/hành trình | Báo cáo kết quả chuẩn |
| **Tổng** | **27** | Chưa tính bìa, mục lục và phụ lục; điều chỉnh theo cách thầy tính trang | |

## Ba câu hỏi nghiên cứu

1. Baseline AOT thay đổi TTID/TTFD ra sao so với chưa AOT, khi dùng cùng APK?
2. Khi đã có Baseline, bổ sung Startup Profile có giảm TTID/TTFD không?
3. Phạm vi thu profile, thứ tự chạy và điều kiện thiết bị ảnh hưởng cách diễn giải kết quả thế nào?

Câu 1: phiên D/E/F trên máy thật có trung vị D/E lần lượt TTID 1068,375/930,032 ms, TTFD 1380,999/1065,297 ms; giảm 12,95%/22,86%.

Câu 2: bộ Startup hiển thị ban đầu có thăm dò EFFE và xác nhận FEEF, 20 lượt/phiên. Thăm dò giảm 20,49%/18,52%; xác nhận giảm 7,66%/11,05%. Báo cả hai, không lấy số thăm dò làm mức luôn đạt được.

Câu 3: giữ kết quả các bộ Startup trước chưa cải thiện; loại phiên người dùng sử dụng điện thoại khỏi kết luận hiệu quả. Việc thu mới cho kết quả tốt hơn không tự chứng minh chỉ thời điểm thu là nguyên nhân duy nhất, vì Baseline hợp nhất và bộ quy tắc cũng thay đổi giữa các cặp build.

## Những điểm phải giữ đúng khi viết

- Hai kỹ thuật là của Android; đóng góp của nhóm là tích hợp, thiết kế đối chứng, đo và phân tích trên app có sẵn. Ghi nguồn leloi04/food-app.
- R8 là phần cần cho Startup và được giữ cố định trong so sánh E/F; lazy cũng giữ nguyên. Không dùng số 14%/25,92% hoặc APK giảm 82,94% của gói tối ưu cũ làm hiệu quả riêng hai profile.
- Kết quả Baseline và Startup ở hai phiên/bộ profile khác nhau; không cộng hai phần trăm hoặc dùng đối chứng chéo phiên.
- TTID là hiển thị ban đầu; TTFD là Login báo vẽ xong, không phải thực đơn/tài khoản đã tải hết.
- Một điện thoại, số lượt nhỏ, biến động lớn; chưa thiết lập ý nghĩa thống kê. Không khẳng định hiệu quả chung cho mọi Android.
- Hình/bảng phải ghi cấu hình, đơn vị ms, số lượt và phiên. Tách kết quả mới khỏi dữ liệu máy ảo cũ.

## Hình, bảng và phụ lục

Sơ đồ build/runtime; cơ chế ART; Baseline/Startup; đường khởi động thực tế. Bảng D/E cho Baseline; bảng E/F của cả hai phiên Startup; biểu đồ chấm từng lượt với đường trung vị; checksum và metadata DEX. Không chép hàng nghìn quy tắc profile vào nội dung chính.

## Công việc nhóm trước khi nộp

Tự viết và kiểm tra diễn giải, bổ sung thông tin thành viên/đóng góp, trích nguồn, đối chiếu số liệu và tập demo. Đây là khung viết, chưa phải báo cáo 27 trang đã hoàn thiện. Sơ đồ trong guide học có thể dùng để hiểu cơ chế; kết luận số liệu lấy từ báo cáo cập nhật, vì README học đang được giữ nguyên theo yêu cầu.
