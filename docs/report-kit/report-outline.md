# Khung báo cáo: 27 trang nội dung

Tên gợi ý: **Nghiên cứu và thực nghiệm tối ưu hiệu năng khởi động ứng dụng Android bằng Baseline Profiles và Startup Profiles trên ứng dụng đặt đồ ăn.**

Trọng tâm thuộc nhóm tối ưu hiệu năng tính toán; nghiên cứu hành trình khởi động và cơ chế biên dịch/profile. Không mở rộng tên thành tối ưu toàn diện bộ nhớ, pin, bảo mật khi chưa đo các đại lượng ấy.

Số trang dưới đây là kế hoạch, không phải bản báo cáo đã viết. Chưa tính bìa, mục lục, danh mục hình và phụ lục; nếu thầy tính tất cả vào 25–30 trang, nhóm rút phần nền tảng/phụ lục tương ứng.

| Chương | Trang | Câu hỏi phải trả lời | Vật liệu có thể đối chiếu |
|---|---:|---|---|
| 1. Bài toán và phạm vi | 2 | Vì sao khởi động quan trọng? Nhóm tối ưu hành trình nào? | Guide phần 4, 10; nêu rõ chưa đăng nhập |
| 2. Cơ sở lý thuyết | 6 | APK/DEX, ART/JIT/AOT, cold/warm/hot, TTID/TTFD, profile khác nhau thế nào? | Guide phần 1–3, 7–10; nguồn Android chính thức |
| 3. Phân tích app trước tối ưu | 4 | Luồng DI kéo theo những khởi tạo nào? Căn cứ từ mã là gì? | MainActivity, AuthViewModel, NetworkModule; sơ đồ 2–3 |
| 4. Triển khai kỹ thuật | 5 | Sửa ở đâu, kiểm soát bằng cờ nào, giữ nguyên hành vi nào? | Guide phần 6–9; source snapshot; sơ đồ 4–7 |
| 5. Thiết kế thí nghiệm | 3 | Đối chứng là gì, chế độ compilation, thiết bị và biến nhiễu? | Ma trận A–G; ABBA; JSON context; sơ đồ 8 |
| 6. Kết quả và thảo luận | 5 | Mức giảm tính thế nào? Dải biến thiên nói gì? Startup Profile có đáng bật? | Raw CSV, bảng thống kê, PNG TTID/TTFD, toàn bộ bảng A–G |
| 7. Kết luận và hướng mở rộng | 2 | Chứng minh được gì/chưa được gì? Bước xác nhận tiếp theo? | Giới hạn: máy ảo, tác động tổng hợp, thao tác đầu tiên |
| **Tổng** | **27** | | |

## Câu hỏi nghiên cứu để nhóm tự diễn đạt

1. Trên hành trình chưa đăng nhập, có khởi tạo nào có thể trì hoãn mà không bỏ kiểm tra phiên?
2. Cấu hình kết hợp lazy + R8 + Baseline AOT thay đổi TTID, TTFD và kích thước APK ra sao trong môi trường đã chọn?
3. Bổ sung Startup Profile có cho lợi ích đủ rõ để bật mặc định trong lượt thử không?

Câu 2 hiện có xác nhận A/E trên máy ảo. Câu 1 được hỗ trợ bằng phân tích mã và triển khai, chưa định lượng riêng lợi ích lazy. Câu 3 mới có thăm dò E/F; kết quả chưa ủng hộ bật mặc định. Không diễn đạt cả ba như đã có bằng chứng nhân quả độc lập.

## Hình, bảng và phụ lục nên có

- Hình build/runtime, luồng mở app thực tế, eager trước sửa, lazy sau sửa, cơ chế ART, quy trình Baseline, Startup Profile, quy trình ABBA.
- Hai biểu đồ chấm các lần đo TTID/TTFD; chú thích đường ngang là trung vị.
- Bảng ánh xạ thay đổi → tệp → cách kiểm tra; bảng cấu hình A–G; bảng A/E trung vị và min/max; dung lượng APK và SHA-256.
- Phụ lục mã trọng tâm, lệnh chạy, đường dẫn dữ liệu gốc, phiên bản thiết bị/công cụ. Không dán 17.749 quy tắc profile vào nội dung chính.

## Những phần nhóm phải tự làm trước nộp

Đọc mã, giải thích bằng cách hiểu của từng thành viên, nêu đóng góp thực tế và ghi nguồn repo ban đầu. Thêm ảnh nhóm tự chạy/thao tác hoặc số đo mới khi có. Kiểm tra đơn vị, số lần, mốc TTFD và giới hạn trong mọi bảng. Tài liệu hỗ trợ này không thay thế bản báo cáo nhóm tự viết theo yêu cầu của thầy.

Nếu không đủ thời gian đo máy thật, giữ nguyên giới hạn nghiên cứu. Nếu có đo thêm, giữ thành một đợt mới với thiết bị/điều kiện riêng; không trộn các lần máy thật và máy ảo rồi tính một trung vị chung.
